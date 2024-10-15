# Project # 3 EAFIT University 2024-2

## Authors

- Miguel Vásquez Bojanini
- Esteban Muriel Roldan
- Manuel Villegas Michel

## Instructions

It checks if FOLDER is a folder that is in the same path where the executable is. For each file with a .csv extension that it finds in the folder, it must load it into memory in an ArrayList or similar. When the process finishes, a summary message is displayed that must indicate:

- Program start time
- Start time of loading the first file
- End time of loading the last file
- Summary table with the duration of loading all the files according to the order in which they were processed
- Time (in mm:ss format) that the entire process took.
- Most popular video of that year.
- Most inpopular video of that year.
- Most popular and inpopular videos by region.
- If you do not have OPTIONS enabled, each file is read one at a time sequentially until it finishes.

### Options

-s: Instructs the program to read the n files with the .csv extension that it finds in the folder at a time, where each file to be read must be assigned to an independent **thread** assigned to the same process in the same core where the initial dataload runs.
-m: Like -s, each process receives a file to be read, but each process can be assigned to any of the cores that the computer has available. Each process uses multiple threads to read a single file by blocks.

## Process exit status:

- 0 If the process ends OK
- 1 If the process ends with errors
