# CSIS3-Assignment2


To Add Data run following:-
1. sudo apt-get update
2. sudo apt install python3-pip
3. gdown https://drive.google.com/uc?id=17KpMCaE34eLvdiTINqj1lmxSBSu8BtDP
4. sudo apt-get install unzip
5. unzip 'Assignment Two.zip' ./Data
6. mv ./Data <path to repo>


To Download Queries File:- 
1. gdown https://drive.google.com/uc?id=1CaCtA2RHhW4DP--5HyHnKm9jjqyWvc99
2. mv ./topics <path to repo>

Running Code:-
1. cd Code/
2. mvn package
CREATE INDEX:-
3. java -jar target/example1-1.2-createindex.jar "english"
SEARCH and CREATE results.txt file
4. java -jar target/example1-1.2-lucenesearch.jar "english" "bm25"

Running trec_eval from main repo folder
1. cd trec_eval-9.0.7
./trec_eval ../qrels-part1.txt ../Code/results.txt 