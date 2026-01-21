#!/bin/bash

# Generate 10 medium-sized CSV files
echo "Generating 10 test CSV files with 100k rows each..."
for i in {1..10}; do
  python3 -c "
with open('test-file-$i.csv', 'w') as f:
    f.write('id,name,amount\n')
    for j in range(100000):
        f.write(f'{j},User{j},{j * 1.5}\n')
" &
done
wait

echo "Files generated. Starting concurrent uploads..."

# Upload all 10 files simultaneously
for i in {1..10}; do
  (
    echo "Starting upload $i..."
    time curl -X POST http://localhost:8080/api/convert \
      -F "file=@test-file-$i.csv" \
      -F "hasHeader=true" \
      -o "output-$i.json" 2>&1 | grep real
    echo "Upload $i completed"
  ) &
done

wait
echo "All uploads completed!"

# Verify outputs
echo ""
echo "Verification:"
for i in {1..10}; do
  if [ -f "output-$i.json" ]; then
    rows=$(jq -r '.metadata.rows_processed' "output-$i.json" 2>/dev/null)
    if [ "$rows" = "100000" ]; then
      echo "✓ output-$i.json: $rows rows processed"
    else
      echo "✗ output-$i.json: Invalid or incomplete"
    fi
  else
    echo "✗ output-$i.json: Missing"
  fi
done
