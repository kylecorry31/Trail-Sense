import pandas as pd
import datetime as dt
import matplotlib.pyplot as plt
import numpy as np
from tkinter import Tk
from tkinter import filedialog

# Create a file dialog window and store the selected file path
root = Tk()
root.withdraw()
file_path = filedialog.askopenfilename()

# Load the data
df = pd.read_csv(file_path)

# Calculate the difference between consecutive timestamps
df['Time'] = pd.to_datetime(df['Time'])
df = df.sort_values('Time')
df['time_diff'] = df['Time'].diff()
df['time_diff'] = df['time_diff'].dt.total_seconds() / 60

# Remove the first row (NaN value)
df = df.iloc[1:]

# Plot histogram
plt.figure(figsize=(10,6))
plt.hist(df['time_diff'], bins=np.arange(0, 30, 1), edgecolor='black')
plt.title('Distribution of Time Differences')
plt.xlabel('Time Difference (minutes)')
plt.ylabel('Frequency')
plt.show()

# Calculate key statistics
mean_diff = df['time_diff'].mean()
median_diff = df['time_diff'].median()
min_diff = df['time_diff'].min()
max_diff = df['time_diff'].max()
std_diff = df['time_diff'].std()
q1_diff = df['time_diff'].quantile(0.25)
q3_diff = df['time_diff'].quantile(0.75)

print(f"Mean Time Difference: {mean_diff}")
print(f"Median Time Difference: {median_diff}")
print(f"Min Time Difference: {min_diff}")
print(f"Max Time Difference: {max_diff}")
print(f"Standard Deviation of Time Difference: {std_diff}")
print(f"1st Quartile Time Difference: {q1_diff}")
print(f"3rd Quartile Time Difference: {q3_diff}")
