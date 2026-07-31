import re
import math

import numpy as np
import networkx as nx
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import nltk
from nltk.tokenize import sent_tokenize

# Ensure NLTK punkt tokenizer data is available
try:
    nltk.data.find('tokenizers/punkt_tab')
except LookupError:
    nltk.download('punkt_tab', quiet=True)


class TextRankService:
    """Service for extractive text summarization using TextRank algorithm
    with Multilingual-E5-Small embeddings and cosine similarity.
    
    The TextRank algorithm works by:
    1. Splitting text into sentences using NLTK
    2. Preprocessing: case folding, remove special characters, normalize space
    3. Adding "query: " prefix for E5 model encoding
    4. Generating sentence embeddings using multilingual-e5-small
    5. Building a similarity graph using cosine similarity
    6. Applying PageRank (TextRank) to rank sentences by importance
    7. Selecting the top N% most important sentences (rounded)
    
    Attributes:
        model: The loaded SentenceTransformer model instance.
    """

    def __init__(self):
        self.model = None

    def load_model(self):
        """Load the multilingual-e5-small model for sentence embeddings.
        
        Should be called on application startup for faster first summarization,
        or will be called automatically on first summarize() call.
        """
        self.model = SentenceTransformer(
            "intfloat/multilingual-e5-small",
            cache_folder="E:\\Untuk Library\\sentence_transformers"
        )

    def split_sentences(self, text: str) -> list:
        """Split text into sentences using NLTK sent_tokenize.
        
        Uses NLTK's Punkt tokenizer for robust sentence segmentation
        that handles abbreviations and edge cases properly.
        Filters out very short fragments (less than 10 characters).
        
        Args:
            text: Input text to split into sentences.
        
        Returns:
            List of sentence strings.
        """
        sentences = sent_tokenize(text.strip())
        return [s.strip() for s in sentences if len(s.strip()) > 10]

    def preprocess(self, sentence: str) -> str:
        """Preprocess a sentence according to thesis specification.
        
        Applies three preprocessing steps:
        1. Case folding - convert to lowercase
        2. Remove special characters - keep only alphanumeric and spaces
        3. Normalize space - trim and collapse multiple spaces
        
        Args:
            sentence: Raw sentence text.
        
        Returns:
            Cleaned and normalized sentence string.
        """
        # Step 1: Case folding (lowercase)
        text = sentence.lower()
        # Step 2: Remove special characters (keep alphanumeric and spaces)
        text = re.sub(r'[^a-zA-Z0-9\s]', '', text)
        # Step 3: Normalize space (trim + collapse multiple spaces)
        text = re.sub(r'\s+', ' ', text).strip()
        return text

    def summarize(self, text: str, percentage: int = 60) -> str:
        """Summarize text using TextRank with E5-Small embeddings.
        
        Implements the TextRank algorithm as described in thesis Bab IV:
        1. Sentence segmentation using NLTK
        2. Preprocessing (case folding, remove special chars, normalize space)
        3. Add "query: " prefix (symmetric comparison within same document)
        4. Generate embeddings using multilingual-e5-small
        5. Build cosine similarity matrix with diagonal = 0
        6. Apply PageRank (damping factor = 0.85)
        7. Select top N% sentences (rounded), return in chronological order
        
        Args:
            text: Input text to summarize.
            percentage: Percentage of sentences to keep in the summary (1-100).
        
        Returns:
            Summary text containing the top-ranked sentences in original order.
        """
        if self.model is None:
            self.load_model()

        sentences = self.split_sentences(text)
        if len(sentences) <= 2:
            return text

        # Preprocessing + prefix "query: " as specified in thesis
        preprocessed = [self.preprocess(s) for s in sentences]
        prefixed = ["query: " + s for s in preprocessed]

        # Generate embeddings
        embeddings = self.model.encode(prefixed)

        # Build similarity matrix using cosine similarity
        sim_matrix = cosine_similarity(embeddings)
        np.fill_diagonal(sim_matrix, 0)

        # Build graph and apply PageRank (TextRank) with d=0.85
        graph = nx.from_numpy_array(sim_matrix)
        scores = nx.pagerank(graph, alpha=0.85)

        # Select top N% sentences (using round() as per thesis calculation)
        n_sentences = max(1, round(len(sentences) * percentage / 100))
        ranked = sorted(scores.items(), key=lambda x: x[1], reverse=True)
        top_indices = sorted([idx for idx, _ in ranked[:n_sentences]])

        # Join selected ORIGINAL sentences (not preprocessed) in chronological order
        summary = " ".join([sentences[i] for i in top_indices])
        return summary


# Singleton instance
textrank_service = TextRankService()
