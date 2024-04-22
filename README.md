# Programming Projects 4
## Consistent Hashing-based Naming Service

### About the Project
This project implements a flat naming system using consistent hashing (CH). It involves a distributed set of servers managing key-value pairs and supporting operations such as insertions, deletions, and lookups. The system includes CH name servers and a specialized bootstrap CH server that offers additional functionalities.

### Consistent Hashing
- Facilitates seamless node integration and removal.
- Provides efficient mechanisms for insertion, lookup, and deletion operations.
- Less efficient compared to a typical Distributed Hash Table.

### Naming Service
- Provides a reliable and uniform way to name resources, enhancing discoverability and interaction through metadata retrieval.

**Note:** Basic implementation details are available within this project's repository.

### About the Repository
The repository is organized into two main directories:
- **Bootstrap Name Server**: Contains files necessary for running the bootstrap server.
- **Name Server**: Contains files necessary for running regular name servers.

### Technology Stack
- **Language**: Java (v11.0.10)
- **Required Environment**: Java (open-jdk)

### How to Run
#### Bootstrap Server
Navigate to the bootstrapServer directory and execute the following commands:
```bash
javac BootstrapServer.java
java BootstrapServer config_example.txt
