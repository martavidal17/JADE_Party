# JADE_Party
# Party Simulation - HostAgent

This program simulates a party where a host agent (HostAgent) invites several guest agents (GuestAgents), spreads rumors among them, and organizes introductions between the guests. The party ends when all guests have heard the rumor.

## Requirements

- Java 8 or later
- [JADE (Java Agent DEvelopment Framework)](https://jade.tilab.com/) (ensure you have the `jade.jar` file in the `lib` folder)

## Main Classes

### `HostAgent`

The `HostAgent` class is responsible for managing the party. It:

- Invites guest agents to the party.
- Starts the rumor spreading process and introduces guests to each other.
- Ends the party when all guests have heard the rumor.

### `GuestAgent`

The `GuestAgent` class represents a guest in the party. It:

- Arrives at the event and registers with the host.
- Spreads the rumor when selected by the host.
- Requests introductions to meet other guests.

### `HostUIFrame`

The `HostUIFrame` class provides a graphical user interface (GUI) to interact with the program. The user can:

- Choose the number of guests attending the party.
- See the current status of the party (e.g., how many guests have arrived, how the rumor is progressing).
- Control the party, such as starting or stopping the event.

## Compilation and Execution

### 1. Compile the Source Code

To compile the source code, open the terminal in the root project directory and run the following command:

```bash
javac -cp lib/jade.jar src/*.java -Xlint
```

This command will compile all .java files in the src folder and show any compiler warnings.


### 2. Run the Application

Once the code is compiled, you can run the program with the following command:

```bash
java -cp ".;lib/jade.jar;src" jade.Boot -gui host:HostAgent
```

This command starts the JADE platform in graphical mode and creates the HostAgent to invite guest agents and manage the party flow.
