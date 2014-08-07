ChessR
======

ChessR is a Lucene based Search tool for indexing and retrieval of chess board positions.
As a first step, the program indexes a collection of PGN chess games. You can then run ad-hoc queries on this index.
The query position is also specified with the help of a PGN file. The board position reached after playing the given sequence of moves in the query PGN file is then used as the query board position. The retrieval engine retrieves board positions from games which led to board positions that are "similar" to the query position.

The program is packaged with a build.xml file and hence is easy to load in Eclipse or Netbeans IDE.
The input to the program is a configuration (java properties) file where you can specify the location of the files (PGN games) to index. You can execute multiple queries in batch by specifying a query PGN file in the configuration file.

For more details about the algorithm, refer to the paper "Retrieval of similar chess positions" published in SIGIR '14.
