# Music Genre Classification based Lyrics

## ABSTRACT
This branch gives a whole overview of the system, a tool designed to classify songs into genres based on their lyrics. I identify key features that represent different musical styles and categorize songs into five genres: Rock, Hip-Hop, Jazz, Country, and Pop. To achieve accurate classification, I explore two methods for embedding lyrical data before training the model:

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

The structure of the data is:
        
        index/song/year/artis/genre/lyrics.

**1. Data Pre-processing**


The dataset was not initially structured to meet our requirements. Some songs lacked genre labels, while others had missing lyrics. Therefore, preprocessing was necessary before fitting the data into any classification model.  

To refine the dataset, I removed irrelevant information, unnecessary syntax, and metadata such as artist names and song release years, leaving only lyrics and genre mappings. This made the dataset more compact and accessible.  

To ensure consistency, I used the **"lingua"** library in Python to filter out non-English songs. Next, I extracted **5,000 songs** from each of the five target genres, creating a balanced dataset for analysis. Songs with extremely short lyrics were removed to maintain meaningful content. Additionally, structural markers like **[chorus]**, **[verse]**, **[x1]**, and **[x2]** were eliminated for simplicity.  

For further text processing, I also tokenized the lyrics using the **"spacy"** library (for Word2Vec-based models), applied **lemmatization**, and removed **punctuation** to improve data quality and ensure better feature extraction for classification.


**2. Data Analysis**


After preprocessing the data, I visualized and analyzed it to identify key features, which is the first crucial step in any machine-learning task. 

**Figure 1: Analysis of the lyrics data**

![Image](https://github.com/user-attachments/assets/1132ed10-dcc4-4d5b-9415-9c7207622acc)
![Image](https://github.com/user-attachments/assets/37719ace-41f6-4dad-87b8-04b82954dadd)


**Figure 2: Word Cloud for each genre**

![Image](https://github.com/user-attachments/assets/895512a1-834b-4cc2-93f4-2ec030afe9de)
![Image](https://github.com/user-attachments/assets/dad16ac4-c50e-4e0b-a15d-582d3cbf50ce)
![Image](https://github.com/user-attachments/assets/bce2c526-2be2-4d48-a0e3-230cd74907e0)
![Image](https://github.com/user-attachments/assets/42943495-a8c1-4563-aa4b-432341890945)
![Image](https://github.com/user-attachments/assets/9b978a0c-5dd3-4dbb-9275-53bde7ab7f7c)

As shown in Figure 1, we evaluated the average length of lyrics across each genre. A interesting observation was that Hip-Hop songs tended to be longer than those in other genres, while the remaining genres had similar average lengths. Following this, we calculated the average number of unique words in the lyrics for each genre. Once again, Hip-Hop stood out, featuring a higher number of unique words compared to the other genres. Finally, we analyzed the most common words used in each genre, as shown in Figure 2. This analysis helped us identify potential correlations between the vocabulary choices in the lyrics and their corresponding genres.

## Training Model

### 1. Word Representation

In text classification tasks, representing words in a numerical format is essential for machine learning models to process and understand the data. I explore and develop two primary techniques for word representation: Bag-of-Words (BoW) and Word Embeddings, each with its own strengths and limitations.

**a) Bag-of-Words (BoW) and TF-IDF**

--- Bag-of-Words (BoW) ---

![Image](https://github.com/user-attachments/assets/f11edb82-ddc4-4742-9ef3-23a25e2fe42c)

The Bag-of-Words model is a simple yet effective method for representing text. In this approach, a document is converted into a vector of word occurrences without considering the order of words. Essentially, it treats text as a collection of individual words, disregarding grammar and word relationships.
Each document is represented as a vector, where:

* Each index in the vector corresponds to a word in the vocabulary.

* The value at each index represents the frequency of that word in the document.

![Image](https://github.com/user-attachments/assets/fcb2dbb4-d573-4875-bc34-03e5d8778fbd)

Despite its simplicity, BoW has limitations, as it does not consider the importance of words or their relationships within the text. This is where TF-IDF comes into play.

--- TF-IDF (Term Frequency-Inverse Document Frequency) ---


TF-IDF is an improved version of BoW that assigns different weights to words based on their importance in a dataset. Instead of treating all words equally, it reduces the influence of commonly used words and highlights words that are more relevant for classification.


![Image](https://github.com/user-attachments/assets/13dacb2c-c8dc-43a9-a210-097ae4ae8a8f)


By using this method, it will reduce the importance of common words (e.g., "the", "is", "and") while giving more weight to rare but meaningful words that help differentiate between classes.

However, TF-IDF does not consider word order or semantic meaning. It treats words independently, without recognizing synonyms or relationships between words. To address this, we use Word Embeddings.

**b) Word Embeddings (Word2Vec)**

Unlike BoW and TF-IDF, word embedding techniques provide a more sophisticated representation of text by capturing the semantic relationships between words. One of the most popular methods for this is Word2Vec.
Word2Vec

Word2Vec is a neural network-based model that represents words as dense vectors in a continuous space. Each word is mapped to a high-dimensional vector, where:
Similar words (e.g., "king" and "queen") have similar vector representations.
Word relationships are preserved through mathematical operations (e.g., "king - man + woman = queen").

A document is then represented as a collection of word vectors, unlike BoW and TF-IDF, which rely on fixed-length vectors based on vocabulary size.
Word2Vec typically uses two architectures:

* Continuous Bag-of-Words (CBOW) – Predicts a word based on its surrounding context.

* Skip-gram – Predicts surrounding words given a single word.

![Image](https://github.com/user-attachments/assets/a9b05f03-4ccd-4529-a259-03c5349c8542)

This technique allows models to understand the context and relationships between words, making it particularly useful for tasks like text classification, sentiment analysis, and machine translation.

### 2. Cosine Similarity





## Result

| Model / Accuracy  | Logistic Regression | Naive Bayes (Multinomial Naive Bayes) | Decision Tree | Random Forest | SVM (Support Vector Machine) |
|--------------|-------|------|-------|-------|-------|
| TF-IDF | 0.6 | 0.60 | 0.48 | 0.63 | 0.61 |
| Word2Vec  | 0.52 | X | 0.46 | 0.58 | 0.55 |


A summarization of the results is demonstrated in the
figure. We tested for with both TF-IDF vectors and counts
as our feature vectors. We observe that TF-IDF vectors are
better representation of the words in the lyrics. And among
the algorithms, Multi Layer Perceptron performed better
than the other algorithms with an overall accuracy of 63.5%
accuracy. SVM comes close second with 61.9% accuracy.
The confusion matrix shows that Hip-Hop is most accurately
classified and Jazz is mislabeled most of the times. Then,
we used word2vec as our feature vector, and applied the
Extra Trees Classifier and Support Vector Machines, and
we observed accuracy of 60.3% and 62.4%. Hence the use
of word2vec did not produce significant improvement in our
problem.


The best result was achieved by Random Forest with TF-IDF. I was expecting Random Forest with word embedding (Word2Vec) to perform better, but it is probably because for this task, the order of the words is not really important, but their appearance is.



![Image](https://github.com/user-attachments/assets/3ab6beff-4b2f-4a14-bbd3-738e5afaef9d)



## Conclusion

