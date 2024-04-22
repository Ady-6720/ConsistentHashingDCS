/**
 * ## Programming Projects 4
 * ### Consistent Hashing-based Naming Service
 * 
 * #### About the Project
 * This project implements a flat naming system using consistent hashing (CH). It involves
 * a distributed set of servers that manage key-value pairs with support for operations such
 * as insertions, deletions, and lookups. The system includes CH name servers and a 
 * specialized bootstrap CH server that offers additional functionalities.
 * 
 * #### Consistent Hashing
 * - Facilitates nodes joining and leaving without major disruptions.
 * - Provides efficient mechanisms for insertion, lookup, and deletion operations.
 * - Not as efficient as a typical Distributed Hash Table.
 * 
 * #### Naming Service
 * - Offers a reliable and uniform way to name resources, allowing their discovery and
 *   interaction through metadata retrieval.
 * 
 * **Note**: Basic implementation details are available within this project's repository.
 * 
 * ### About the Repository
 * The repository is organized into two folders:
 * - `Bootstrap Name Server`: Contains files for the bootstrap server.
 * - `Name Server`: Contains files for regular name servers.
 * 
 * ### Technology Stack
 * - Language: Java (v11.0.10)
 * - Required Environment: Java (open-jdk)
 * 
 * ### How to Run
 * #### Bootstrap Server
 * Navigate to the bootstrapServer directory and execute:
 * ```bash
 * $ javac BootstrapServer.java
 * $ java BootstrapServer config_example.txt
 * ```
 * The server will start and await connections.
 * 
 * #### Name Server
 * In a separate terminal, navigate to the nameServer directory and execute:
 * ```bash
 * $ javac NameServer.java
 * $ java NameServer config_example.txt
 * ```
 * Configuration files are included in the repository.
 * 
 * ### Implemented Functionalities
 * - **Lookup**: Retrieves the associated value for a given key.
 * - **Insert**: Adds a new key-value pair to the system.
 * - **Delete**: Removes a key-value pair from the system.
 * 
 * ### Assumptions
 * - Keys are integers in the range [0, 1023].
 * - Values are alphanumeric strings without special characters.
 * - The CH name server IDs are also in the range [0, 1023].
 * - The Bootstrap name server, with ID 0, is a permanent, central node.
 * - Operations (lookup, insert, delete) start from the bootstrap name server.
 * - Servers communicate using socket connections.
 * - Consistent hashing is used, with each server knowing only its immediate neighbors.
 * - Bootstrap name server is known to all other servers.
 * - Server interactions (entry/exit) begin at the bootstrap server and proceed clockwise.
 * 
 * ### Honor Pledge
 * This project was done in its entirety by Aditya Malode and Yash Joshi. 
 * We hereby state that we have not received unauthorized help of any form.
 * 
 * #### Members
 * - Yash Joshi (811420683)
 * - Aditya Malode (811656637)
 */
