package coinffeine.peer.bitcoin.blockchain

import java.util
import scala.collection.JavaConversions._

import com.typesafe.scalalogging.StrictLogging
import org.bitcoinj.core.AbstractBlockChain.NewBlockType
import org.bitcoinj.core._

import coinffeine.model.bitcoin.{Hash, ImmutableTransaction}

private[blockchain] class BlockchainNotifier(initialHeight: Int)
  extends AbstractBlockChainListener with StrictLogging {
  import BlockchainNotifier._

  private case class BlockIdentity(hash: Hash, height: Int)

  /** Subscription to the confirmation of a given transaction.
    *
    * @param txHash                 Hash of the transaction to observe
    * @param listener               Who to notify
    * @param requiredConfirmations  How deep the transaction should be
    * @param foundInBlock           Block in which the transaction is included on the blockchain
    *                               (if ever)
    */
  private case class ConfirmationSubscription(
      txHash: Hash,
      listener: ConfirmationListener,
      requiredConfirmations: Int,
      foundInBlock: Option[BlockIdentity] = None)

  private case class HeightSubscription(height: Long, listener: HeightListener)

  private var confirmationSubscriptions: Map[Sha256Hash, ConfirmationSubscription] = Map.empty
  private var heightSubscriptions: Set[HeightSubscription] = Set.empty
  private var outputSubscriptions: Map[TransactionOutPoint, Set[OutputListener]] =
    Map.empty.withDefaultValue(Set.empty)
  private var currentHeight = initialHeight

  def watchTransactionConfirmation(tx: Hash,
                                   confirmations: Int,
                                   listener: ConfirmationListener): Unit = synchronized {
    logger.debug(s"Subscribing to $tx, waiting for $confirmations confirmations")
    confirmationSubscriptions += tx -> ConfirmationSubscription(tx, listener, confirmations)
  }

  def watchHeight(height: Long, listener: HeightListener): Unit = synchronized {
    if (height <= currentHeight) {
      logger.debug(s"Already at $currentHeight height no need to watch for $height")
      listener.heightReached(currentHeight)
    } else {
      logger.debug(s"Watching height until block $height")
      heightSubscriptions += HeightSubscription(height, listener)
    }
  }

  def watchOutput(output: TransactionOutPoint, listener: OutputListener): Unit = synchronized {
    logger.debug(s"Watching $output output")
    val updatedListeners = outputSubscriptions(output) + listener
    outputSubscriptions += output -> updatedListeners
  }

  override def notifyNewBestBlock(block: StoredBlock): Unit = synchronized {
    currentHeight = block.getHeight
    logger.trace(s"New block ${block.getHeader.getHash} at height $currentHeight arrived")
    notifyConfirmedTransactions()
    notifyHeight()
  }

  override def reorganize(splitPoint: StoredBlock, oldBlocks: util.List[StoredBlock],
                          newBlocks: util.List[StoredBlock]): Unit = synchronized {
    logger.warn(s"Blockchain reorganization from height ${splitPoint.getHeight}")
    val seenTxs = confirmationSubscriptions.values.filter(_.foundInBlock.isDefined)
    val rejectedTransactions = seenTxs.filterNot(tx =>
      newBlocks.toSeq.exists(block => block.getHeader.getHash == tx.foundInBlock.get.hash))
    rejectedTransactions.foreach { subscription =>
      logger.warn("Tx {} is lost in blockchain reorganization; reporting to the observer",
        subscription.txHash)
      confirmationSubscriptions -= subscription.txHash
      subscription.listener.rejected(subscription.txHash)
    }

    /* It seems to be a bug in Bitcoinj that causes the newBlocks list to be in an arbitrary
     * order although the Javadoc of BlockChainListener says it follows a top-first order.
     * Thus, we have to sort the blocks from the list to determine the correct order.
     */
    newBlocks.sortBy(_.getHeight).foreach(notifyNewBestBlock)
  }

  override def isTransactionRelevant(tx: Transaction): Boolean = synchronized {
    confirmationSubscriptions.contains(tx.getHash) || isSpendingWatchedOutputs(tx)
  }

  private def isSpendingWatchedOutputs(tx: Transaction): Boolean = synchronized {
    val spentOutputs = tx.getInputs.map(_.getOutpoint).toSet
    outputSubscriptions.keySet.intersect(spentOutputs).nonEmpty
  }

  override def receiveFromBlock(tx: Transaction, block: StoredBlock,
                                blockType: NewBlockType, relativityOffset: Int): Unit = synchronized {
    updateConfirmationSubscriptions(tx.getHash, block)
    notifySpentOutputs(tx)
  }

  private def updateConfirmationSubscriptions(tx: Hash, block: StoredBlock): Unit = {
    logger.debug(s"tx $tx found in block ${block.getHeight}")
    confirmationSubscriptions.get(tx).foreach { subscription =>
      logger.info(s"tx $tx found in block ${block.getHeight}: " +
        s"waiting for ${subscription.requiredConfirmations} confirmations")
      confirmationSubscriptions += tx -> subscription.copy(
        foundInBlock = Some(BlockIdentity(block.getHeader.getHash, block.getHeight)))
    }
  }

  private def notifySpentOutputs(tx: Transaction): Unit = {
    if (outputSubscriptions.nonEmpty) {
      logger.trace(s"Checking for ${tx.getHashAsString} spending watched outputs. TX=$tx")
    }
    for (input <- tx.getInputs) {
      val outPoint = input.getOutpoint
      val relevantSubscriptions = outputSubscriptions(outPoint)
      relevantSubscriptions.foreach { subscription =>
        subscription.outputSpent(ImmutableTransaction(tx))
      }
      if (relevantSubscriptions.nonEmpty) {
        logger.info("{} was spent by {}", outPoint, tx.getHashAsString)
        outputSubscriptions -= outPoint
      }
    }
  }

  override def notifyTransactionIsInBlock(txHash: Sha256Hash,
                                          block: StoredBlock,
                                          blockType: NewBlockType,
                                          relativityOffset: Int): Boolean = {
    logger.trace(s"Transaction $txHash in block ${block.getHeader.getHashAsString} at height ${block.getHeight}")
    updateConfirmationSubscriptions(txHash, block)
    confirmationSubscriptions.contains(txHash)
  }

  private def notifyConfirmedTransactions(): Unit = {
    confirmationSubscriptions.foreach {
      case (txHash, ConfirmationSubscription(_, listener, reqConf, Some(foundInBlock))) =>
        val confirmations = (currentHeight - foundInBlock.height) + 1
        val event =
          s"""after new chain head $currentHeight, tx $txHash have $confirmations
             |confirmations out of $reqConf required""".stripMargin
        if (confirmations >= reqConf) {
          logger.info("{}: reporting to the observer", event)
          confirmationSubscriptions -= txHash
          listener.confirmed(txHash, confirmations)
        } else {
          logger.info("{}: still waiting for more blocks", event)
        }

      case (_, subscription) => logger.trace(s"Still waiting for $subscription")
    }
  }

  private def notifyHeight(): Unit = {
    heightSubscriptions.foreach { case subscription @ HeightSubscription(height, listener) =>
      if (currentHeight >= height) {
        logger.info(s"Watched height $height was reached")
        heightSubscriptions -= subscription
        listener.heightReached(currentHeight)
      }
    }
  }
}

private[blockchain] object BlockchainNotifier {
  trait ConfirmationListener {
    def rejected(tx: Hash): Unit
    def confirmed(tx: Hash, confirmations: Int): Unit
  }

  trait HeightListener {
    def heightReached(currentHeight: Long): Unit
  }

  trait OutputListener {
    def outputSpent(tx: ImmutableTransaction): Unit
  }
}
