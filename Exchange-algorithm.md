## Introduction

This document describes the Bitmarket protocol with enough detail to create a compatible implementation. The document is structured in two parts: the first one is a high-level description in which actors, roles and purpose of operations is described, the second one analyzes the protocol as a game played by rational actors and the last one provides the low-level details of the actions in terms of bitcoin transactions and scripts.

## Exchanging bitcoins and other currency

The BitMarket protocol is designed to allow two independent individuals to interchange bitcoins (or other alt-coins) and regular money automatically in a scenario of very low trust.

Let's say Bob is willing to buy 1 BTC at 100 € and Sam is willing to sell it to Bob at that exchange rate.

### Trust scenario

In a scenario in which Bob and Sam trust each other, they can just follow the following steps (steps 2 and 3 are interchangeable):

1. Bob and Sam tell each other their bitcoin address and bank account respectively
2. Bob transfers 100 € to to Sam’s account
3. Sam signs a transaction of 1 BTC to the address of Bob

This naïve approach doesn’t work in absence of trust: the first party that pays is at the mercy of the other one that could refuse to further collaborate.

### Trusted third parties

A typical solution to the problem of lack of trust is to have a trusted third party that receives the payments and redistributes them again after deducting a fee. Currently, a handful of bitcoin exchanges (BTC China, MtGox, BitStamp) play this role in most of the bitcoin / traditional money exchanges.

The trusted third party solution has several disadvantages. These exchanges act as a single point of failure, which position them as a highly desired target for both hackers and the government, which can target the different bitcoin exchanges to prevent people from buying bitcoins.

### Peer-to-peer solution

Bitmarket offers an peer-to-peer solution to enable currency exchange between bitcoins and other currencies in the absence of trust between the actors that does not involve a trusted third party. The next section provides a high level overview of how this is accomplished.

# STILL NEED TO ADD A BUNCH OF STUFF