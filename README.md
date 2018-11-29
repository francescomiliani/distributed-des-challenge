## Distributed DES Challenge

The original DES challenge was launched in January 1997, with the aim of demonstrating that 56-bit security, such as that offered by the US government's DES, offers only marginal protection against a committed adversary. This was confirmed when the secret key used for encryption was recovered on June 17, 1997. Since then it has been widely acknowledge that much faster exhaustive search efforts are possible and DES challenge II is intended to show how fast.
While the original showed DES was crackable using an exhaustive search attack, the goal of the new DES challenge is to see how quickly an exhaustive search attack can be accomplished to help judge the true vulnerability of DES.
Twice a year, on January 13 and July 13, at 9:00 AM Pacific Time, a new contest will be posted on the RSA homepage. The contest will consist of the ciphertext produced by DES-encrypting some unknown plaintext message that has a fixed and known message header. The first to recover the key wins, and the amount of the prize will depend on how fast the key was recovered.

**DES challenge details**

For each contest, the unknown message will be preceded by three known blocks of text containing the 24-character phrase: ``The unknown message is:``. While the mystery text that follows will clearly be known to a few employees of the RSA Data Security, the secret key actually used for the encryption will be generated at random and destroyed within the challenge-generating software. The key will never be revealed to anyone.
The goal of each contest is for participants to recover the secret randomly generated key that was used on the encryption in a faster time than that required for earlier challenges in the series.

**Breaking DES**

Many problems require a large amount of computational power to reach to a solution. Some problems, though, are amenable to an extremely high level of parallelization, and with today's Internet it is possible to broaden the reach of any large-scale effort to previously unanticipated levels.
Breaking DES is one of these problems. It is necessary to use a somewhat large computational power. Even if we consider a 56-bit key it is very difficult and hard task to perform.
The "best" strategy to break DES is to perform a brute force attack. This means having to test all the possible keys and analyse and compare the results. If we consider a 56-bit key, meaning that we will have approximately 256 possible combinations. Even with today common computational power this takes a while.
Breaking DES is clearly a NP-complete problem. It is impossible to find a solution to a NP-complete problem in polynomial time, but given a solution, it is possible to check whether it is valid or not. Typically, all cryptographic problems are NP-complete problems.

**The Basis Idea**

A server for distributing the keys and the clients to do the hard work: test all the keys and check the result.

---

## How to use this program

Server exposes a GUI in order to receive command and provide information about its current state
Commands:

1. **“ciphertext”, “plaintext”** → provide the ciphertext and the plaintext of the challenge
2. **vol_num** → number of Volunteers currently connected to the server
3. **vol_list** → returns the list of Volunteers and their associated blocks
4. **state** → returns the state of the search and the percentage of the key space explored
5. **close** → if the challenge is finished, shuts down the server

