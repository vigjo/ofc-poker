from flask import Flask
from flask import render_template

app = Flask(__name__)

@app.route("/")
def hello_world():
    source = ""
    source += "<a href=/game>Play Game</a><br>"
    source += "<p>Hello, World!</p>"
    source += "<g id=\"king_spade\">text</g>"
    return source

@app.route("/game")
def play_game():
    source = ""
    source += "<a href=/>Home</a><br>"
    source += "woof"
    return render_template("base.html")