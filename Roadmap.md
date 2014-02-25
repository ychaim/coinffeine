We are in an early stage of the implementation in which some components are implemented but we don't have a complete working program but what is written has a high level of test coverage, often written in TDD style.

The high-level objectives for the next versions are listed here, for the details filter the [issue list](/Coinffeine/coinffeine/issues) by milestone.

* **Version 0.1**. First version in which command line peers will be able to perform BTC-fiat exchanges using OKPay (as first supported payment processor) in a distributed fashion. Limitations for this version: price negotiation will be performed in a centralized server and peer communication will be direct and based on protocol buffers RPC, limited parametrization of the process.

* **Version 0.2**. Remove some of the constraints.

 * Replace protobuf RPC by DHT routing
 * Introduce a richer order book
 * Introduce a GUI

* **Version 0.3** Get ready for beta testing.

 * Implement a [[broker set]] to maintain the order book in a distributed fashion.

* ...