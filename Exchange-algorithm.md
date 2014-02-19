## Introduction

This document describes the Coinffeine exchange protocol. The document is structured in several sections:

- [Exchanging bitcoins and other currency](#wiki-exchanging-bitcoins-and-other-currency): a high level overview of the different approaches to exchanging bitcoins for other currency.
- [Protocol overview](#wiki-protocol-overview): a high-level description of the exchange protocol.
- [Transaction definitions](#wiki-transaction-definitions): an explanation of the protocol at the bitcoin transaction level.
- [Game theory properties](#wiki-game-theory-properties): analysis of the protocol as a game played by rational actors

## Exchanging bitcoins and other currency

The Coinffeine protocol is designed to allow two independent individuals to exchange bitcoins and regular money automatically in a scenario of very low trust.

Let's say Bob is willing to buy 1 BTC at 100 € and Sam is willing to sell it to Bob at that exchange rate.

### Trust scenario

In a scenario in which Bob and Sam trust each other, they can just follow the following steps (steps 2 and 3 are interchangeable):

1. Bob and Sam tell each other their bitcoin address and bank account respectively
2. Bob transfers 100 € to to Sam's account
3. Sam signs a transaction of 1 BTC to the address of Bob

This naïve approach doesn’t work in absence of trust: the first party that pays is at the mercy of the other one that could refuse to further collaborate.

### Trusted third parties

A typical solution to the problem of lack of trust is to have a trusted third party that receives the payments and redistributes them again after deducting a fee. Currently, a handful of bitcoin exchanges (BTC China, MtGox, BitStamp) play this role in most of the bitcoin / traditional money exchanges.

The trusted third party solution has several disadvantages. These exchanges act as a single point of failure, which position them as a highly desired target for both hackers and the government, which can target the different bitcoin exchanges to prevent people from buying bitcoins.

### Peer-to-peer solution

Coinffeine offers an peer-to-peer solution to enable currency exchange between bitcoins and other currencies in the absence of trust between the actors that does not involve a trusted third party. The next section provides a high level overview of how this is accomplished.

## Protocol overview

Coinffeine uses a [bitcoin contract](https://en.bitcoin.it/wiki/Contracts) known as _Micropayment Channel_ to create deposits which force users to continue to cooperate in the transaction if they want to recover the deposit money.

Let's go back to Bob, who wants to buy 1 BTC at 100€, and Sam, who wants to sell 1 BTC at 100€. Bob and Sam find each other through the P2P network and decide they want to engage in a transaction to exchange Sam's bitcoin for Bob's 100€. In order to minimize risk they decide they want to perform the exchange 1€ and 0.01 BTC at a time

They formalize this intent by creating a deposit that expires after a certain amount of time (let's say 1 hour). That is, they are blocking some funds for the next hour, which is the time they will have to perform the exchange. Once the hour is up, they are free to redeem their deposits if the exchange did not happen.

# STILL NEED TO ADD A BUNCH OF STUFF