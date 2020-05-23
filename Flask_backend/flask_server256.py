from flask import Flask, request, jsonify
from markupsafe import escape

import tensorflow as tf
from keras import preprocessing as kp

import re
import numpy as np
import os
import io

import json


app = Flask(__name__)

def preprocess_sentence(sentence):
  sentence = sentence.lower().strip()
  # creating a space between a word and the punctuation following it
  # eg: "he is a boy." => "he is a boy ."
  sentence = re.sub(r"([?.!,])", r" \1 ", sentence)
  sentence = re.sub(r'[" "]+', " ", sentence)
  # replacing everything with space except (a-z, A-Z, ".", "?", "!", ",")
  sentence = re.sub(r"[^a-zA-Z?.!,]+", " ", sentence)
  sentence = re.sub(r"i'm", "i am", sentence)
  sentence = re.sub(r"he's", "he is", sentence)
  sentence = re.sub(r"she's", "she is", sentence)
  sentence = re.sub(r"that's", "that is", sentence)
  sentence = re.sub(r"what's", "what is", sentence)
  sentence = re.sub(r"where's", "where is", sentence)
  sentence = re.sub(r"how's", "how is", sentence)
  sentence = re.sub(r"\'ll", " will", sentence)
  sentence = re.sub(r"\'ve", " have", sentence)
  sentence = re.sub(r"\'re", " are", sentence)
  sentence = re.sub(r"\'d", " would", sentence)
  sentence = re.sub(r"n't", " not", sentence)
  sentence = re.sub(r"won't", "will not", sentence)
  sentence = re.sub(r"can't", "cannot", sentence)
  sentence = sentence.strip()
  sentence = ' '.join(sentence.split()[:50])
  # adding a start and an end token to the sentence
  sentence = '<start> ' + sentence + ' <end>'
  return sentence


with open(os.path.join(os.path.dirname(__file__),"input_tokenizer1.json")) as f:
    data = json.load(f)
    input_tokenizer = kp.text.tokenizer_from_json(data)

with open(os.path.join(os.path.dirname(__file__),"target_tokenizer1.json")) as f:
    data = json.load(f)
    target_tokenizer = kp.text.tokenizer_from_json(data)

class Encoder(tf.keras.layers.Layer):
  def __init__(self, vocab_size, embedding_dim, enc_units, batch_sz):
    super(Encoder, self).__init__()
    self.batch_sz = batch_sz
    self.enc_units = enc_units
    self.embedding = tf.keras.layers.Embedding(vocab_size, embedding_dim)
    self.gru = tf.keras.layers.GRU(self.enc_units,
                                   return_sequences=True,
                                   return_state=True,
                                   recurrent_initializer='glorot_uniform')

  def call(self, x, hidden):
    x = self.embedding(x)
    output, state = self.gru(x, initial_state = hidden)
    return output, state

  def initialize_hidden_state(self):
    return tf.zeros((self.batch_sz, self.enc_units))

class BahdanauAttention(tf.keras.layers.Layer):
  def __init__(self, units):
    super(BahdanauAttention, self).__init__()
    self.W1 = tf.keras.layers.Dense(units)
    self.W2 = tf.keras.layers.Dense(units)
    self.V = tf.keras.layers.Dense(1)

  def call(self, query, values):
    # query hidden state shape == (batch_size, hidden size)
    # query_with_time_axis shape == (batch_size, 1, hidden size)
    # values shape == (batch_size, max_len, hidden size)
    # we are doing this to broadcast addition along the time axis to calculate the score
    query_with_time_axis = tf.expand_dims(query, 1)

    # score shape == (batch_size, max_length, 1)
    # we get 1 at the last axis because we are applying score to self.V
    # the shape of the tensor before applying self.V is (batch_size, max_length, units)
    score = self.V(tf.nn.tanh(
        self.W1(query_with_time_axis) + self.W2(values)))

    # attention_weights shape == (batch_size, max_length, 1)
    attention_weights = tf.nn.softmax(score, axis=1)

    # context_vector shape after sum == (batch_size, hidden_size)
    context_vector = attention_weights * values
    context_vector = tf.reduce_sum(context_vector, axis=1)

    return context_vector, attention_weights

class Decoder(tf.keras.layers.Layer):
  def __init__(self, vocab_size, embedding_dim, dec_units, batch_sz):
    super(Decoder, self).__init__()
    self.batch_sz = batch_sz
    self.dec_units = dec_units
    self.embedding = tf.keras.layers.Embedding(vocab_size, embedding_dim)
    self.gru = tf.keras.layers.GRU(self.dec_units,
                                   return_sequences=True,
                                   return_state=True,
                                   recurrent_initializer='glorot_uniform')
    self.fc = tf.keras.layers.Dense(vocab_size)

    # used for attention
    self.attention = BahdanauAttention(self.dec_units)

  def call(self, x, hidden, enc_output):
    # enc_output shape == (batch_size, max_length, hidden_size)
    context_vector, attention_weights = self.attention(hidden, enc_output)

    # x shape after passing through embedding == (batch_size, 1, embedding_dim)
    x = self.embedding(x)

    # x shape after concatenation == (batch_size, 1, embedding_dim + hidden_size)
    x = tf.concat([tf.expand_dims(context_vector, 1), x], axis=-1)

    # passing the concatenated vector to the GRU
    output, state = self.gru(x)

    # output shape == (batch_size * 1, hidden_size)
    output = tf.reshape(output, (-1, output.shape[2]))

    # output shape == (batch_size, vocab)
    x = self.fc(output)

    return x, state, attention_weights

class seq2seq(tf.keras.Model):
  def __init__(self,vocab_inp_size,vocab_tar_size,embedding_dim,units,batch_size,attention_units,name='seq2seq',**kwargs):
    super(seq2seq, self).__init__(name=name, **kwargs)
    self.encoder = Encoder(vocab_inp_size, embedding_dim, units, batch_size)

    self.attention_layer = BahdanauAttention(attention_units)

    self.decoder = Decoder(vocab_tar_size, embedding_dim, units, batch_size)

   
    
  
  
  def call(self,example_input_batch,batch_size):
    self.sample_hidden = self.encoder.initialize_hidden_state()
    self.sample_output, self.sample_hidden = self.encoder(example_input_batch, self.sample_hidden)
    self.attention_result, self.attention_weights = self.attention_layer(self.sample_hidden, self.sample_output)
    self.sample_decoder_output, _, _ = self.decoder(tf.random.uniform((batch_size, 1)),
                                      self.sample_hidden, self.sample_output)

  def evaluate(self,sentence):
    sentence = preprocess_sentence(sentence)
  
    
    inputs = []
    for i in sentence.split(' '):
      if(input_tokenizer.word_index.get(i)):
        inputs.append(input_tokenizer.word_index.get(i))
    
      
    inputs = tf.keras.preprocessing.sequence.pad_sequences([inputs],
                                                           maxlen=max_length_inp,
                                                           padding='post')
    inputs = tf.convert_to_tensor(inputs)
  
    result = ''
  
    hidden = [tf.zeros((1, units))]
    enc_out, enc_hidden = self.encoder(inputs, hidden)
  
    dec_hidden = enc_hidden
    dec_input = tf.expand_dims([target_tokenizer.word_index['<start>']], 0)
  
    for t in range(max_length_targ):
      predictions, dec_hidden, attention_weights = self.decoder(dec_input,
                                                           dec_hidden,
                                                           enc_out)
  
      predicted_id = tf.argmax(predictions[0]).numpy()
  
      if target_tokenizer.index_word[predicted_id] == '<end>':
        return result, sentence
  
      result += target_tokenizer.index_word[predicted_id] + ' '
  
      
  
      # the predicted ID is fed back into the model
      dec_input = tf.expand_dims([predicted_id], 0)
  
    return result, sentence
  
  def reply(self, sentence):
    result, sentence = self.evaluate(sentence)
  
    return result

vocab_inp_size = 30930
vocab_tar_size =30591
BATCH_SIZE = 1
embedding_dim = 256
units = 256
max_length_inp = 12
max_length_targ = 12

example_input_batch = tf.zeros(
    ([BATCH_SIZE, 12]), dtype=tf.dtypes.int32
)

model = seq2seq(vocab_inp_size,vocab_tar_size,embedding_dim,units,BATCH_SIZE,10)

model(example_input_batch,BATCH_SIZE)

model.load_weights(os.path.join(os.path.dirname(__file__),"model256.h5"))




# root
@app.route("/")
def index():
    """
    this is a root dir of my server
    :return: str
    """
    return "This is root!!!!"




@app.route('/reply/<query>')
def reply_user(query):
    """
    this serves as a demo purpose
    :param user:
    :return: str
    """
    reply = model.reply(query)
    
    return {'reply':reply}


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
