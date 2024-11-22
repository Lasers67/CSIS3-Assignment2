A2.py
import os
import random
import numpy as np
import torch
from transformers import BertTokenizer, BertModel, LlamaTokenizer, LlamaModel
from sklearn.metrics.pairwise import cosine_similarity
import json
from torch.utils.data import DataLoader
import os
import json
import pandas as pd
import re
#hyperparameters
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(device)
g_batch_size = 256


RandomSeed = 52
random.seed(RandomSeed)
torch.manual_seed(RandomSeed)


model_name  = 'bert-base-uncased'
tokenizer = BertTokenizer.from_pretrained('bert-base-uncased')
model = BertModel.from_pretrained('bert-base-uncased')
model.to(device)

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

    # Move the inputs to the appropriate device (e.g., GPU if available)
    inputs = {key: value.to(device) for key, value in inputs.items()}

    # Get the hidden states from the model without gradients
    with torch.no_grad():
        outputs = model(**inputs)

    # Use mean pooling over token embeddings to get a single document embedding
    mean_pooled_embedding = outputs.last_hidden_state.mean(dim=1).cpu().numpy()  # Move tensor to CPU and convert to NumPy
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
        documents=[]
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



class DocumentBatchReader:
    def __init__(self, file_paths, batch_size=g_batch_size):
        self.file_paths = file_paths
        self.batch_size = batch_size
        self.current_file_index = 0

    def _read_all_from_file(self, file_path):
        # Read all documents from a single file
        return read_files_dataset(file_path)

    def get_next_batch(self):
        # Initialize an empty list to store batches
        batch = []
        # Get up to 4 files in a single batch
        for _ in range(1):
            if self.current_file_index < len(self.file_paths):
                file_path = self.file_paths[self.current_file_index]
                batch.extend(self._read_all_from_file(file_path))
                # Move to the next file for the next batch call
                self.current_file_index += 1
            else:
                break  # No more files to read
        # Return batch if there are any files in it; otherwise, return None
        return batch if batch else None




def preprocess_batch(batch):
    return tokenizer(
        batch,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=512
    ).to(device)

def generate_batch_embeddings(batch):
    with torch.no_grad():
        outputs = model(**batch)
    # Extract [CLS] embeddings for the batch
    return outputs.last_hidden_state[:, 0, :]

# List all your files (assuming they are XML files and stored in a directory)
file_paths = [f"./Data/latimes/{file_name}" for file_name in os.listdir('./Data/latimes/') if not file_name.endswith('.txt')]
#file_paths = [
#    f"./Data/fbis/{folder_name}/{file_name}"
#    for folder_name in os.listdir('./Data/fbis/')
#    if not folder_name.endswith('.txt')
#    for file_name in os.listdir(f'./Data/ft/{folder_name}/')
#]
# Create a batch reader instance
batch_reader = DocumentBatchReader(file_paths, batch_size=g_batch_size)
#output_file_embeddings = './Embeddings/latimes/latimes-embeddings_'
output_file_embeddings = './query/query-embeddings_'
#output_file_documents = './Embeddings/latimes/latimes-documents_'
output_file_documents = './Embeddings/query-documents_'
counter = 1

# Prepare lists to store documentIDs and texts separately
document_ids_data = []
texts_data = []

while True:
    batch = batch_reader.get_next_batch()
    if batch:
        # Step 1: Split the documents into two lists
        document_ids = []
        texts=[]
        for doc in batch:
            document_ids.append(doc['documentID'])
            if 'text' in doc:
                texts.append(doc['text'])
            else:
                texts.append("")
        print("Current Processed Files",len(texts_data))
        all_embeddings = []
        data_loader = DataLoader(texts, batch_size=192, shuffle=False, collate_fn=list)
        for batch in data_loader:
            tokenized_batch = preprocess_batch(batch)
            batch_embeddings = generate_batch_embeddings(tokenized_batch)
            all_embeddings.append(batch_embeddings)
        # Step 2: Get embeddings for the text batch
        all_embeddings = torch.cat(all_embeddings, dim=0)
        # Step 3: Store documentID and embedding separately
        document_ids_data.extend(document_ids)
        texts_data.extend(all_embeddings)
        # Save to files if the data exceeds 10,000 entries
        if len(document_ids_data) > 10000:
            # Save documentIDs
            output_file_ids = output_file_documents + str(counter) + '.txt'
            with open(output_file_ids, 'w') as f:
                for doc in document_ids_data:
                  f.write(doc+"\n")

            # Save embeddings
            output_file_texts = output_file_embeddings + str(counter) + '.txt'
            with open(output_file_texts, 'w') as f:
                for doc in texts_data:
                    doc = doc.cpu().numpy()
                    f.write(" ".join(map(str, doc)) + "\n")

            print(f"Document IDs saved to {output_file_ids}.")
            print(f"Embeddings saved to {output_file_texts}.")

            # Increment counter and clear lists
            counter += 1
            document_ids_data = []
            texts_data = []
    else:
        # Save documentIDs
        output_file_ids = output_file_documents + str(counter) + '.txt'
        with open(output_file_ids, 'w') as f:
            for doc in document_ids_data:
                  f.write(doc+"\n")

        # Save embeddings
        output_file_texts = output_file_embeddings + str(counter) + '.txt'
        with open(output_file_texts, 'w') as f:
            for doc in texts_data:
                  f.write(" ".join(map(str, doc)) + "\n")

        print(f"Document IDs saved to {output_file_ids}.")
        print(f"Embeddings saved to {output_file_texts}.")
        print("No more documents available.")
        break