#!/bin/bash

# JUnit Test Summary Viewer
# Displays a summary of test execution results from Surefire reports

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/backend/target/surefire-reports"
FAILSAFE_DIR="$SCRIPT_DIR/backend/target/failsafe-reports"

# Colors
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_BLUE}╔════════════════════════════════════════════╗${COLOR_RESET}"
echo -e "${COLOR_BLUE}║   JUnit Test Execution Summary             ║${COLOR_RESET}"
echo -e "${COLOR_BLUE}╚════════════════════════════════════════════╝${COLOR_RESET}"
echo ""

# Check if reports exist
if [ ! -d "$REPORT_DIR" ] || [ -z "$(ls -A $REPORT_DIR/*.txt 2>/dev/null)" ]; then
    echo -e "${COLOR_YELLOW}⚠ No test reports found.${COLOR_RESET}"
    echo "Run tests first: cd backend && mvn test"
    exit 1
fi

# Parse text reports
TOTAL_TESTS=0
TOTAL_FAILURES=0
TOTAL_ERRORS=0
TOTAL_SKIPPED=0
TOTAL_TIME=0
FAILED_CLASSES=()

while IFS= read -r line; do
    if [[ $line =~ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+),\ Errors:\ ([0-9]+),\ Skipped:\ ([0-9]+),\ Time\ elapsed:\ ([0-9.]+) ]]; then
        tests=${BASH_REMATCH[1]}
        failures=${BASH_REMATCH[2]}
        errors=${BASH_REMATCH[3]}
        skipped=${BASH_REMATCH[4]}
        time=${BASH_REMATCH[5]}
        
        TOTAL_TESTS=$((TOTAL_TESTS + tests))
        TOTAL_FAILURES=$((TOTAL_FAILURES + failures))
        TOTAL_ERRORS=$((TOTAL_ERRORS + errors))
        TOTAL_SKIPPED=$((TOTAL_SKIPPED + skipped))
        TOTAL_TIME=$(echo "$TOTAL_TIME + $time" | bc 2>/dev/null || echo "$TOTAL_TIME")
        
        if [ "$failures" -gt 0 ] || [ "$errors" -gt 0 ]; then
            FAILED_CLASSES+=("$line")
        fi
    fi
done < <(grep "Tests run:" "$REPORT_DIR"/*.txt 2>/dev/null)

# Display overall summary
echo -e "${COLOR_BLUE}Overall Summary:${COLOR_RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
printf "  Tests run:    %-6s\n" "$TOTAL_TESTS"

if [ "$TOTAL_FAILURES" -gt 0 ]; then
    printf "  Failures:     ${COLOR_RED}%-6s${COLOR_RESET}\n" "$TOTAL_FAILURES"
else
    printf "  Failures:     ${COLOR_GREEN}%-6s${COLOR_RESET}\n" "$TOTAL_FAILURES"
fi

if [ "$TOTAL_ERRORS" -gt 0 ]; then
    printf "  Errors:       ${COLOR_RED}%-6s${COLOR_RESET}\n" "$TOTAL_ERRORS"
else
    printf "  Errors:       ${COLOR_GREEN}%-6s${COLOR_RESET}\n" "$TOTAL_ERRORS"
fi

if [ "$TOTAL_SKIPPED" -gt 0 ]; then
    printf "  Skipped:      ${COLOR_YELLOW}%-6s${COLOR_RESET}\n" "$TOTAL_SKIPPED"
else
    printf "  Skipped:      ${COLOR_GREEN}%-6s${COLOR_RESET}\n" "$TOTAL_SKIPPED"
fi

if [ -n "$TOTAL_TIME" ] && [ "$TOTAL_TIME" != "0" ]; then
    printf "  Total time:   %-6.2f s\n" "$TOTAL_TIME"
fi

# Calculate success rate
if [ "$TOTAL_TESTS" -gt 0 ]; then
    PASSED=$((TOTAL_TESTS - TOTAL_FAILURES - TOTAL_ERRORS - TOTAL_SKIPPED))
    SUCCESS_RATE=$(echo "scale=1; $PASSED * 100 / $TOTAL_TESTS" | bc 2>/dev/null || echo "0")
    echo -e "  Success rate: ${COLOR_GREEN}${SUCCESS_RATE}%${COLOR_RESET}"
fi

echo ""

# Display per-class summary
echo -e "${COLOR_BLUE}Per Test Class:${COLOR_RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for txt_file in "$REPORT_DIR"/*.txt; do
    if [ -f "$txt_file" ]; then
        class_name=$(basename "$txt_file" .txt)
        summary=$(grep "Tests run:" "$txt_file" | head -1)
        
        if echo "$summary" | grep -q "Failures: [1-9]\|Errors: [1-9]"; then
            echo -e "${COLOR_RED}✗${COLOR_RESET} $class_name"
            echo "  $summary"
        else
            echo -e "${COLOR_GREEN}✓${COLOR_RESET} $class_name"
            echo "  $summary"
        fi
    fi
done

echo ""

# Show failures/errors if any
if [ ${#FAILED_CLASSES[@]} -gt 0 ]; then
    echo -e "${COLOR_RED}⚠ Failures/Errors Detected:${COLOR_RESET}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    for failed in "${FAILED_CLASSES[@]}"; do
        echo -e "${COLOR_RED}$failed${COLOR_RESET}"
    done
    echo ""
    echo "View detailed reports:"
    echo "  cd backend/target/surefire-reports"
    echo "  cat <test-class-name>.txt"
fi

# Show failsafe summary if available
if [ -f "$FAILSAFE_DIR/failsafe-summary.xml" ]; then
    echo ""
    echo -e "${COLOR_BLUE}Failsafe Summary (Integration Tests):${COLOR_RESET}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat "$FAILSAFE_DIR/failsafe-summary.xml" | grep -E "<completed>|<errors>|<failures>|<skipped>" | sed 's/</ /g; s/>/ /g; s/^/  /'
fi

echo ""
echo -e "${COLOR_BLUE}Report Locations:${COLOR_RESET}"
echo "  Text reports:  backend/target/surefire-reports/*.txt"
echo "  XML reports:   backend/target/surefire-reports/TEST-*.xml"
echo "  Failsafe:      backend/target/failsafe-reports/failsafe-summary.xml"
echo ""

