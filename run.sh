#!/bin/bash

# 1. Validate script arguments
if [ "$#" -ne 3 ]; then
    echo "Error: Missing port arguments."
    echo "Usage: $0 <port1> <port2> <port3>"
    echo "Example: $0 6001 6002 6003"
    exit 1
fi

PORT1=$1
PORT2=$2
PORT3=$3

# 2. Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in your PATH."
    echo "Please install Maven to compile and run this project."
    exit 1
fi

echo "Preparing to compile using Maven and fetching dependencies..."

# 3. Compile the project AND copy all external JARs to target/dependency
mvn clean compile dependency:copy-dependencies

# Check if compilation was successful
if [ $? -ne 0 ]; then
    echo "Maven compilation failed! Please check your code."
    exit 1
fi

# 4. Set the classpath to include both your classes AND the external JARs
# The colon (:) separates multiple classpath directories in Unix/macOS
CLASSPATH="target/classes:target/dependency/*"

echo "Compilation successful! Output directory is set to: $CLASSPATH"

# 5. Handle graceful shutdown (kills all background tasks if you press Ctrl+C)
trap 'echo -e "\nShutting down all processes..."; kill $(jobs -p) 2>/dev/null; exit' INT TERM EXIT

echo "Starting infrastructure nodes in the background..."
java -cp "$CLASSPATH" srg.Srg &
java -cp "$CLASSPATH" reducer.Reducer &

# Brief pause to allow infrastructure to initialize
sleep 1

echo "Starting Master Node (Ports: $PORT1, $PORT2, $PORT3)..."
java -cp "$CLASSPATH" master.Master $PORT1 $PORT2 $PORT3 &

# Wait for Master to bind to its ports before workers try to connect
sleep 2

echo "Starting Worker Nodes..."
java -cp "$CLASSPATH" worker.Worker $PORT1 &
java -cp "$CLASSPATH" worker.Worker $PORT2 &
java -cp "$CLASSPATH" worker.Worker $PORT3 &

# Brief pause before launching the console
sleep 1

echo "======================================================="
echo "All backend nodes are running in the background."
echo "Starting ManagerConsole in the foreground..."
echo "Press Ctrl+C at any time to terminate the entire system."
echo "======================================================="

# 6. Start the Console in the foreground
java -cp "$CLASSPATH" manager.ManagerConsole