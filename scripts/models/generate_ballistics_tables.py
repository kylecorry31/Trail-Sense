import os

# Download https://www.jbmballistics.com/ballistics/downloads/text/olin.txt to olin.txt in this directory
with open(os.path.join(os.path.dirname(__file__), 'olin.txt'), 'r', encoding='utf-8') as f:
    data = f.read()
lines = data.splitlines()[1:]
values = [[float(v.strip().replace('*', '')) for v in line.split('\t')] for line in lines if line.strip() and '\t' in line]

v_table = [row[0] for row in values]
g1_table = [row[1] for row in values]

feet_to_meters = 0.3048

# G1
for i in range(len(v_table)):
    print(f"{v_table[i] * feet_to_meters}f to {g1_table[i] * v_table[i] * feet_to_meters}f,")