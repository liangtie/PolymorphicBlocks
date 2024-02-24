# JLC Parts
This is to demonstrate how JLC Parts Library can be rendered in the doc. 
This is only for demo purpose. Loading the first 1000 rows. 
It can load the entire csv, but the website crashes. 

- [x] You can sort
- [x] You can use search bar (and get highlighted in yellow, once clicked)

!!! warning 

    Currently some of the letters are not rendered properly

    Probably separate the parts table to per category to avoid crash


## JLC Parts List


{{ read_csv('electronics_lib/resources/Pruned_JLCPCB SMT Parts Library(20220419).csv', encoding='latin1', nrows=1000) }}
