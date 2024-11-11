import os
import random
import numpy as np
import torch
from transformers import BertTokenizer, BertModel, LlamaTokenizer, LlamaModel
from sklearn.metrics.pairwise import cosine_similarity
import json

#TODO
#1 TRY ADDING BYLINE AND HEADER
#2 TRY fine-tuning model somehow

#hyperparameters
batch_size = 256

RandomSeed = 52
random.seed(RandomSeed)
torch.manual_seed(RandomSeed)


model_name  = 'bert-base-uncased'
tokenizer = BertTokenizer.from_pretrained('bert-base-uncased')
model = BertModel.from_pretrained('bert-base-uncased')


#model_name = "meta-llama/Llama-3.2-1B"
#tokenizer = LlamaTokenizer.from_pretrained(model_name)
#model = LlamaModel.from_pretrained(model_name)

def get_document_embedding(text):
    # Tokenize the input text
    inputs = tokenizer.batch_encode_plus(
            text,                	
            padding=True,          	
            truncation=True,       	
            return_tensors='pt',  	
            add_special_tokens=True)
    # Get the hidden states from the model
    with torch.no_grad():  # Disable gradients to save memory and speed up computations
        outputs = model(**inputs)
    # Use the mean pooling over token embeddings to get a single document embedding
    mean_pooled_embedding = outputs.last_hidden_state.mean(dim=1).squeeze().numpy()
    return mean_pooled_embedding

def read_files_dataset(file_path):
    documents = []
    
    try:
        # Try reading the file with utf-8 encoding, if it fails, fall back to ISO-8859-1
        with open(file_path, 'r', encoding='utf-8') as file:
            document = {}
            in_text = False
            in_headline = False
            in_byline = False
            in_doc = False
            
            for line in file:
                line = line.strip()
                
                # Check if it's the start of a new document
                if line.startswith("<DOCNO>"):
                    document['documentID'] = re.sub(r'<.*?>', '', line).strip()
                elif line.startswith("<HEADLINE>"):
                    in_headline = True
                    headline_parts = []
                    while in_headline:
                        line = next(file).strip()
                        if line.startswith("</HEADLINE>"):
                            in_headline = False
                        else:
                            headline_parts.append(re.sub(r'<.*?>', '', line))
                    #document['headline'] = ' '.join(headline_parts).strip()
                elif line.startswith("<BYLINE>"):
                    in_byline = True
                    byline_parts = []
                    while in_byline:
                        line = next(file).strip()
                        if line.startswith("</BYLINE>"):
                            in_byline = False
                        else:
                            byline_parts.append(re.sub(r'<.*?>', '', line))
                    #document['byLine'] = ' '.join(byline_parts).strip()
                elif line.startswith("<TEXT>"):
                    in_text = True
                    text_parts = []
                    while in_text:
                        line = next(file).strip()
                        if line.startswith("</TEXT>"):
                            in_text = False
                        else:
                            text_parts.append(re.sub(r'<.*?>', '', line))
                    document['text'] = ' '.join(text_parts).strip()
                
                # End of a document
                elif line.startswith("</DOC>"):
                    documents.append(document)
                    document = {}

    except UnicodeDecodeError as e:
        print(f"UnicodeDecodeError: {e}")
        print(f"Trying ISO-8859-1 encoding for file: {file_path}")
        try:
            # Try opening the file with ISO-8859-1 encoding if utf-8 fails
            with open(file_path, 'r', encoding='ISO-8859-1') as file:
                document = {}
                in_text = False
                in_headline = False
                in_byline = False
                in_doc = False
                
                for line in file:
                    line = line.strip()
                    
                    # Check if it's the start of a new document
                    if line.startswith("<DOCNO>"):
                        document['documentID'] = re.sub(r'<.*?>', '', line).strip()
                    elif line.startswith("<HEADLINE>"):
                        in_headline = True
                        headline_parts = []
                        while in_headline:
                            line = next(file).strip()
                            if line.startswith("</HEADLINE>"):
                                in_headline = False
                            else:
                                headline_parts.append(re.sub(r'<.*?>', '', line))
                        #document['headline'] = ' '.join(headline_parts).strip()
                    elif line.startswith("<BYLINE>"):
                        in_byline = True
                        byline_parts = []
                        while in_byline:
                            line = next(file).strip()
                            if line.startswith("</BYLINE>"):
                                in_byline = False
                            else:
                                byline_parts.append(re.sub(r'<.*?>', '', line))
                        #document['byLine'] = ' '.join(byline_parts).strip()
                    elif line.startswith("<TEXT>"):
                        in_text = True
                        text_parts = []
                        while in_text:
                            line = next(file).strip()
                            if line.startswith("</TEXT>"):
                                in_text = False
                            else:
                                text_parts.append(re.sub(r'<.*?>', '', line))
                        document['text'] = ' '.join(text_parts).strip()
                    
                    # End of a document
                    elif line.startswith("</DOC>"):
                        documents.append(document)
                        document = {}

        except Exception as ex:
            print(f"Failed to read file {file_path} with both UTF-8 and ISO-8859-1 encodings.")
            print(f"Error: {ex}")
    
    return documents

#CREATE DOCUMENTS
class DocumentBatchReader:
    def __init__(self, file_paths, batch_size=batch_size):
        self.file_paths = file_paths
        self.batch_size = batch_size
        self.current_file_index = 0
        self.current_line_index = 0
        self.documents = []
    
    def _read_next_batch_from_file(self, file_path):
        # Read documents from a single file and return a batch of size self.batch_size
        documents_in_file = read_files_dataset(file_path)
        
        # Return the next batch from the current file
        batch = []
        while len(batch) < self.batch_size and self.current_line_index < len(documents_in_file):
            batch.append(documents_in_file[self.current_line_index])
            self.current_line_index += 1
        return batch
    
    def get_next_batch(self):
        # Return the next batch of documents across multiple files if necessary
        batch = []
        
        # Loop through files to gather documents until we have enough for the batch
        while len(batch) < self.batch_size and self.current_file_index < len(self.file_paths):
            file_path = self.file_paths[self.current_file_index]
            
            # Read the next batch from the current file
            file_batch = self._read_next_batch_from_file(file_path)
            batch.extend(file_batch)
            
            # If we've finished the current file, move to the next one
            if self.current_line_index >= len(read_files_dataset(file_path)):
                self.current_file_index += 1
                self.current_line_index = 0
        
        return batch if len(batch) > 0 else None


# List all your files (assuming they are XML files and stored in a directory)
file_paths = [f"./Data/fbis/{file_name}" for file_name in os.listdir('./Data/fbis') if not file_name.endswith('.txt')]
# Create a batch reader instance
print(file_paths)
batch_reader = DocumentBatchReader(file_paths, batch_size=batch_size)

# Prepare a list to store the documents with their embeddings
embeddings_data = []

while(1):
    batch = batch_reader.get_next_batch()
    if batch:
        # Step 1: Split the documents into two lists
        document_ids = [doc['documentID'] for doc in batch]
        texts = [doc['text'] for doc in batch]
        # Step 2: Get embeddings for the text batch
        embeddings = get_document_embedding(texts)
        # Step 3: Join the lists back together with documentID and embedding
        for i, doc in enumerate(batch):
            doc['text'] = embeddings[i]  # Replace text with its embedding vector
        print(batch)
    else:
        print("No more documents available.")
        break

output_file = './fbis-embeddings.json'
with open(output_file, 'w') as f:
    json.dump(embeddings_data, f)
print(f"Embeddings saved to {output_file}.")