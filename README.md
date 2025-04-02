# Music Genre Classification based Lyrics

## ABSTRACT
This branch gives a whole perspective on how to build a system that can identify the genre of a song based on its lyrics. I identify key features that represent different musical styles and categorize songs into five genres: Rock, Hip-Hop, Jazz, Country, and Pop. To achieve accurate classification, I explore two methods for embedding lyrical data before training the model:

* TF-IDF (Term Frequency-Inverse Document Frequency)
* Word2Vec

Users can input the lyrics of a song, and the program will analyze the content to predict its genre. If users wish to transcribe a song for prediction, they must first create a folder named "songs_input", save the song file there, and then provide the file's location for processing.
=> This system leverages natural language processing techniques to understand lyrical patterns, offering an insightful way to classify music genres based on textual content.

## Introduction
Music is an expression of emotion, and different genres emerge from the variety of human emotions. People often search for songs based on genre, making genre classification an essential aspect of Music Information Retrieval.

This project explores a system that predicts a song's genre based on its lyrics by identifying key stylistic features unique to each genre. Since lyrics contain patterns such as rhyme schemes, slang usage, and structural complexity, they can serve as a valuable basis for classification. However, genre classification using only lyrics is a challenging Natural Language Processing (NLP) problem, as traditional models like SVM, KNN, and Naïve Bayes struggle with clearly distinguishing between multiple genres.

To address this, I focus on five genres:

* Rock
* Hip-Hop
* Jazz
* Country
* Pop 

I apply supervised learning techniques for classification, comparing various models and analyzing their performance. Among the tested methods, Random Forest with TF-IDF achieves the best results, effectively predicting genres based on lyrics.

## DATASET
Dataset for this problem were not abundant mostly due to copyright issues. However, after comparing datasets from
several sources, I found out a data set in Kaggle which was most suited for our purpose. The dataset is basically a collection of 380000+ lyrics from songs scraped from metrolyrics.com.

**1. Data Pre-processing**


The dataset was not initially structured to meet our requirements. Some songs lacked genre labels, while others had missing lyrics. Therefore, preprocessing was necessary before fitting the data into any classification model.  

To refine the dataset, I removed irrelevant information, unnecessary syntax, and metadata such as artist names and song release years, leaving only lyrics and genre mappings. This made the dataset more compact and accessible.  

To ensure consistency, I used the **"lingua"** library in Python to filter out non-English songs. Next, I extracted **5,000 songs** from each of the five target genres, creating a balanced dataset for analysis. Songs with extremely short lyrics were removed to maintain meaningful content. Additionally, structural markers like **[chorus]**, **[verse]**, **[x1]**, and **[x2]** were eliminated for simplicity.  

For further text processing, I also tokenized the lyrics using the **"spacy"** library (for Word2Vec-based models), applied **lemmatization**, and removed **punctuation** to improve data quality and ensure better feature extraction for classification.


**2. Data Analysis**

Figure 1: Word Cloud for each genre

![Image](https://github.com/user-attachments/assets/895512a1-834b-4cc2-93f4-2ec030afe9de)
<img src="https://prnt.sc/wiC1cLeBC94F">
<img src="https://prnt.sc/H05AqlOLzN5M">
<img src="https://prnt.sc/W6JMoU8k2Vi4">
<img src="https://prnt.sc/f3mk0Lf9IbJd">

Figure 2: Analysis of the lyrics data

<img src="">
<img src="">

## Training Model



## Result

**Accurancy**
|  | Logistic Regression | Naive Bayes (Multinomial Naive Bayes) | Decision Tree | Random Forest | SVM (Support Vector Machine) |
|--------------|-------|------|-------|-------|-------|
| TF-IDF | 0.6 | 0.60 | 0.48 | 0.63 | 0.61 |
| Word2Vec  | 0.52 | X | 0.46 | 0.58 | 0.55 |


## Conclusion

