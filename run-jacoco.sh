#!/bin/bash

# JaCoCo Coverage Runner Script
# This script helps you run JaCoCo coverage analysis manually

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"

# Colors for output
COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_YELLOW='\033[1;33m'
COLOR_RESET='\033[0m'

# Find Maven
if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
elif [ -f "/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" ]; then
    MVN_CMD="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
else
    echo "Error: Maven not found. Please ensure Maven is installed or IntelliJ IDEA is installed."
    exit 1
fi

echo -e "${COLOR_BLUE}=== JaCoCo Code Coverage Runner ===${COLOR_RESET}"
echo ""

cd "$BACKEND_DIR"

# Parse command line arguments
COMMAND="${1:-help}"

case "$COMMAND" in
    test|run)
        echo -e "${COLOR_BLUE}Running tests with coverage...${COLOR_RESET}"
        $MVN_CMD clean test
        echo ""
        echo -e "${COLOR_GREEN}✓ Tests completed. Generating coverage report...${COLOR_RESET}"
        $MVN_CMD jacoco:report
        echo ""
        echo -e "${COLOR_GREEN}✓ Coverage report generated!${COLOR_RESET}"
        echo ""
        echo "Report location: $BACKEND_DIR/target/site/jacoco/index.html"
        echo ""
        echo "To view the report, run:"
        echo "  ./run-jacoco.sh view"
        ;;
    
    report)
        echo -e "${COLOR_BLUE}Generating coverage report...${COLOR_RESET}"
        $MVN_CMD jacoco:report
        echo ""
        echo -e "${COLOR_GREEN}✓ Report generated!${COLOR_RESET}"
        echo "Location: $BACKEND_DIR/target/site/jacoco/index.html"
        ;;
    
    check)
        echo -e "${COLOR_BLUE}Checking coverage thresholds...${COLOR_RESET}"
        $MVN_CMD jacoco:check
        ;;
    
    view|open)
        REPORT_FILE="$BACKEND_DIR/target/site/jacoco/index.html"
        if [ -f "$REPORT_FILE" ]; then
            echo -e "${COLOR_BLUE}Opening coverage report...${COLOR_RESET}"
            if [[ "$OSTYPE" == "darwin"* ]]; then
                open "$REPORT_FILE"
            elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                xdg-open "$REPORT_FILE"
            else
                echo "Please open manually: $REPORT_FILE"
            fi
        else
            echo -e "${COLOR_YELLOW}Report not found. Generating it now...${COLOR_RESET}"
            $MVN_CMD test jacoco:report
            if [ -f "$REPORT_FILE" ]; then
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    open "$REPORT_FILE"
                elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                    xdg-open "$REPORT_FILE"
                fi
            fi
        fi
        ;;
    
    clean)
        echo -e "${COLOR_BLUE}Cleaning coverage data...${COLOR_RESET}"
        $MVN_CMD clean
        echo -e "${COLOR_GREEN}✓ Cleaned${COLOR_RESET}"
        ;;
    
    verify)
        echo -e "${COLOR_BLUE}Running full build with coverage (tests + integration tests)...${COLOR_RESET}"
        $MVN_CMD clean verify
        echo ""
        echo -e "${COLOR_GREEN}✓ Build completed with coverage!${COLOR_RESET}"
        echo "Report location: $BACKEND_DIR/target/site/jacoco/index.html"
        ;;
    
    help|--help|-h)
        echo "Usage: ./run-jacoco.sh [command]"
        echo ""
        echo "Commands:"
        echo "  test, run    - Run tests and generate coverage report (default)"
        echo "  report       - Generate coverage report only (requires tests to be run first)"
        echo "  check        - Check coverage against thresholds"
        echo "  view, open   - Open the coverage report in browser"
        echo "  clean        - Clean coverage data and reports"
        echo "  verify       - Run full build with tests and integration tests"
        echo "  help         - Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./run-jacoco.sh           # Run tests with coverage"
        echo "  ./run-jacoco.sh view      # Open coverage report"
        echo "  ./run-jacoco.sh check     # Check coverage thresholds"
        echo ""
        echo "Report location: $BACKEND_DIR/target/site/jacoco/index.html"
        ;;
    
    *)
        echo -e "${COLOR_YELLOW}Unknown command: $COMMAND${COLOR_RESET}"
        echo "Run './run-jacoco.sh help' for usage information"
        exit 1
        ;;
esac

