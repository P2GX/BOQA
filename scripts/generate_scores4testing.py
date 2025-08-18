import random 
import pandas as pd 


alpha = [0.0001,0.001,0.005,0.01,0.05,0.1,0.2,0.5]
beta =  [0.0001,0.001,0.005,0.01,0.05,0.1,0.2,0.5]
num_cases = 50


# Generate 4 lists of random integer counts between 0 and 100
count1 = [random.randint(0,100) for _ in range(num_cases)]
count2 = [random.randint(0,100) for _ in range(num_cases)]
count3 = [random.randint(0,100) for _ in range(num_cases)]
count4 = [random.randint(0,100) for _ in range(num_cases)]

entries = []
for a in alpha:
    for b in beta:
        for i in range(num_cases):
            score = a**count1[i] * b**count2[i] * (1-a)**count3[i] * (1-b)**count4[i]
            # create an entry with alpha, beta, the 4 counts and the score and which will be one line of the dataframe
            entry = {
                'alpha': a,
                'beta': b,
                'count_a': count1[i],
                'count_b': count2[i],
                'count_1ma': count3[i],
                'count_1mb': count4[i],
                'score': score
            }
            entries.append(entry)

columns=['alpha', 'beta', 'count_a', 'count_b', 'count_1ma', 'count_1mb', 'score']
df = pd.DataFrame(entries, columns=columns)
df.to_csv("boqa-core/src/test/resources/com/github/p2gx/boqa/core/boqascores.csv", index=False)