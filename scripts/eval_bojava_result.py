import json
import pandas as pd
import matplotlib.pyplot as plt

def parse_boqa_json(filename: str) -> pd.DataFrame:
    """Parse BOQA JSON file and extract results into a DataFrame"""
    
    # Load the JSON file
    with open(filename, 'r') as f:
        data = json.load(f)
    
    # Extract metadata for reference
    metadata = data['metadata']
    algorithm_params = metadata['algorithmParams']
    
    # List to store all rows
    rows = []
    
    # Process each patient result
    for result in data['results']:
        patient_id = result['patientData']['id']
        
        # Get diagnosis if available
        diagnosis = None
        if 'diagnosis' in result['patientData'] and result['patientData']['diagnosis']:
            diagnosis_info = result['patientData']['diagnosis'][0]
            diagnosis = f"{diagnosis_info['id']}"
        
        # Process each BOQA result for this patient
        for i, boqa_result in enumerate(result['boqaResults']):
            counts = boqa_result['counts']
            
            row = {
                'patient_id': patient_id,
                'diagnosis': diagnosis,
                'rank': i + 1,  # Rank within this patient's results
                'disease_id': counts['diseaseId'],
                'disease_label': counts['diseaseLabel'],
                'tp_count': counts['tpBoqaCount'],
                'fp_count': counts['fpBoqaCount'],
                'tn_count': counts['tnBoqaCount'],
                'fn_count': counts['fnBoqaCount'],
                'boqa_score': boqa_result['boqaScore'],
                'alpha': algorithm_params['alpha'],
                'beta': algorithm_params['beta']
            }
            rows.append(row)
    
    # Create DataFrame
    df = pd.DataFrame(rows)
    
    return df, metadata

# Usage
#filename = '/Users/leonardo/git/BOQA/results/fulltest1.json'
#filename = '/Users/leonardo/git/BOQA/al002_2ndtest.json'
#filename = '/Users/leonardo/git/BOQA/al0005_3rdtest.json'
#filename = '/Users/leonardo/git/BOQA/results/al0001_4thdtest.json'
#filename = '/Users/leonardo/git/BOQA/a1ov40k_5thdtest.json'
#filename = '/Users/leonardo/git/BOQA/a1ov80k_6thdtest.json'
#filename = '/Users/leonardo/git/BOQA/results/a1ov160k_7thdtest.json'
#filename = '/Users/leonardo/git/BOQA/results/a1ov320k_8thtest.json'
#filename = '/Users/leonardo/git/BOQA/a1ov320k_b01_9thtest.json'
#filename = '/Users/leonardo/git/BOQA/a1ov320k_b2_10thtest.json'
#filename = '/Users/leonardo/git/BOQA/a1ov320k_b4_11thtest.json'
#filename = '/Users/leonardo/git/BOQA/a1ov320k_b6_12thtest.json'
filename = '/Users/leonardo/git/BOQA/results/a1ov320k_b8_13thtest.json'
#filename = '/Users/leonardo/git/BOQA/annot_prop_1st_a01b08.json'
#filename = '/Users/leonardo/git/BOQA/annot_prop_1st_a0001b08.json'
#filename = '/Users/leonardo/git/BOQA/annot_prop_1st_a0p001b0p001.json'
#filename = '/Users/leonardo/git/BOQA/annot_prop_1st_a0p0001b0p001.json'
#filename = '/Users/leonardo/git/BOQA/annot_prop_a1ov320k_b0p8.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a0p0001_b0p1.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a0p05_b0p3.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov640k_b0p1.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov640k_b0p001.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov640k_b0p9.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov640k_b0p99.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov640k_b0p6.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov640k_b0p8.json'
#filename = '/Users/leonardo/git/BOQA/results/full_annot_prop_a1ov1p2M_b0p8.json'
#filename = '/Users/leonardo/git/BOQA/results/a1ov640_b0p9.json'

df, metadata = parse_boqa_json(filename)

# Display basic info
print(f"Total rows: {len(df)}")
print(f"Unique patients: {df['patient_id'].nunique()}")
print(f"Algorithm parameters: alpha={metadata['algorithmParams']['alpha']}, beta={metadata['algorithmParams']['beta']}")

# Show first few rows
print("\nFirst 10 rows:")
print(df.head(10))

# Save to CSV if needed
df.to_csv('boqa_results_parsed.csv', index=False)
print("\nSaved to 'boqa_results_parsed.csv'")

# Filter to only include rows where the diagnosis matches the predicted disease
correct_predictions = df[df['diagnosis'] == df['disease_id']]

print(f"Patients with correct predictions: {correct_predictions['patient_id'].nunique()}")

# Plot rank distribution for correct predictions only
rank_counts = correct_predictions['rank'].value_counts().sort_index()

plt.figure(figsize=(15, 5))

# First subplot: Raw counts
plt.subplot(1, 2, 1)
bars = plt.bar(rank_counts.index, rank_counts.values, alpha=0.7, color='steelblue')
plt.xlabel('Rank')
plt.ylabel('Count')
plt.title('Distribution of Ranks for Correct Disease Predictions')
plt.grid(axis='y', alpha=0.3)

plt.legend()

# Add value labels on top of bars
for i, v in enumerate(rank_counts.values):
    plt.text(rank_counts.index[i], v + max(rank_counts.values) * 0.01, str(v), 
                ha='center', va='bottom')

# Second subplot: Cumulative accuracy percentages
plt.subplot(1, 2, 2)

total_patients = df['patient_id'].nunique()

# Calculate cumulative accuracies
top_k_values = [1, 3, 5, 10]
cumulative_counts = []
cumulative_percentages = []

for k in top_k_values:
    count = rank_counts[rank_counts.index <= k].sum()
    percentage = (count / total_patients) * 100
    cumulative_counts.append(count)
    cumulative_percentages.append(percentage)

bars = plt.bar([f'Top-{k}' for k in top_k_values], cumulative_percentages, 
                alpha=0.7, color='darkgreen')
plt.xlabel('Rank Threshold')
plt.ylabel('Accuracy (%)')
plt.title('Cumulative Accuracy by Top-K')
plt.grid(axis='y', alpha=0.3)
plt.ylim(0, 100)

# Add percentage labels on top of bars
for i, (count, percentage) in enumerate(zip(cumulative_counts, cumulative_percentages)):
    plt.text(i, percentage + 1, f'{percentage:.1f}%\n({count}/{total_patients})', 
                ha='center', va='bottom')

plt.tight_layout()
plt.savefig('boqa_accuracy_analysis.png', dpi=300, bbox_inches='tight')
plt.show()

print(f"\nRank distribution for correct predictions:")
print(rank_counts)

# Calculate and print accuracy metrics
print(f"\nCumulative accuracy metrics:")
for k, count, percentage in zip(top_k_values, cumulative_counts, cumulative_percentages):
    print(f"Top-{k} accuracy: {count}/{total_patients} ({percentage:.2f}%)")
    
