from enum import Enum 
#import numpy as np

class Suit(Enum):
    HEART = 1
    SPADE = 2
    DIAMOND = 3
    CLUB = 4

class Rank(Enum):
    ONE = 1,
    TWO = 2,
    THREE = 3,
    FOUR = 4,
    FIVE = 5,
    SIX = 6,
    SEVEN = 7,
    EIGHT = 8
    NINE = 9,
    TEN = 10,
    JACK = 11
    QUEEN = 12
    KING = 13
    ACE = 14

class TopRowRoyalty(Enum):
    PAIR66 = 1
    PAIR77 = 2
    PAIR88 = 3
    PAIR99 = 4
    PAIRTT = 5
    PAIRJJ = 6
    PAIRQQ = 7
    PAIRKK = 8
    PAIRAA = 9
    SET222 = 10
    SET333 = 11
    SET444 = 12
    SET555 = 13
    SET666 = 14
    SET777 = 15
    SET888 = 16
    SET999 = 17
    SETTTT = 18
    SETQQQ = 20
    SETKKK = 21
    SETAAA = 22
    
class MiddleRowRoyalty(Enum):
    TRIPS = 2
    STRAIGHT = 4
    FLUSH = 8
    FULLHOUSE = 12
    QUADS = 20
    STRAIGHTFLUSH = 30
    ROYALFLUSH = 50

class BottomRowRoyalty(Enum):
    STRAIGHT = 4
    FLUSH = 4
    FULLHOUSE = 6
    QUADS = 10
    STRAIGHTFLUSH = 15
    ROYALFLUSH = 25

#do suits need to be modifiable?
#may only need a constructor here, cards shouldnt need to be modified after initialization
class Card:
    def __init__(self, Suit, Rank):
        self.suit = Suit.name
        self.rank = Rank.name
        print(self.suit + " " + self.rank)
            
    def get_suit(self):
        return self.suit
    
    def get_rank(self):
        return self.rank
    

#need to initialize
#draw, shuffle
class Deck:
    def __init__(self):
        self.cards = []

    #TODO - implement mersenne twister randomness into this
    def shuffle(self):
        for suit in list(Suit):
            print(suit)
            for rank in list(Rank):
                new_card = Card(suit, rank)
                self.cards.append(new_card)


class Hand:
    def __init__(self):
        self.cards = []
    
    #def draw(self):


class Board:
    def __init__(self):
        #board is 2x5 and 1x3, treated as 3x5 for now
        #self.cards = [[0]*5]*3
        self.top_row = [0]*3
        self.middle_row = [0]*5
        self.bottom_row = [0]*5
    
    def calculate_value(self):
        return 0    

    

class Player:
    def __init__(self, name):
        self.name = name
        self.board = Board()
        self.hand = Hand()
        self.points = 0

    

class Game:
    def __init__(self):
        self.deck = Deck()
        #deck needs to be shuffled between cycles
        self.deck.shuffle()

        self.deal_round = 0
        self.players = []
        
    #game setup
    def add_player(self, player_name):
        #print(self.players)
        #print(self.players.count.)
        new_player = Player(player_name)
        self.players.append(new_player)
        # if self.players.__len__ < 3:
        #     new_player = Player(player_name)
        #     self.players.append(new_player)
        # else:
        #     print("Player limit already reached")    

    #game playing phase
    def deal(self):
        counter = 0
        for i in range(5):
            for player in self.players:
                player.hand.cards.append(self.deck.cards[counter])
                counter += 1
        self.deal_round += 1        
                
    #player number, range from 0-2, 0-3, need to add something for row management
    #card_index is of range 0-4, reduces by one for each card played
    #player should only be able to play cards that are left in their hand
    def play_card(self, player_number, card_index, col, row):
        print(self.players[player_number].hand.cards)
        if row == 0 and col <= 3:
            self.players[player_number].board.top_row[col] = self.players[player_number].hand.cards.pop(card_index)                       
        elif row == 1 and col <= 5:
            self.players[player_number].board.middle_row[col] = self.players[player_number].hand.cards.pop(card_index)
        elif row == 2 and col <= 5:
            self.players[player_number].board.bottom_row[col] = self.players[player_number].hand.cards.pop(card_index)
        else:
            print("invalid card location")        


    def print_board(self):
        for player in self.players:
            print("player hand: " + player.name)
            for card in player.hand.cards:
                print("card: " + card.suit)
                print("rank: " + card.rank)

            print("player board: ")
            print("top row")
            for card in player.board.top_row:
                self.print_card(card)
                
            print("middle row")
            for card in player.board.middle_row:
                self.print_card(card)
                
            print("bottom row")
            for card in player.board.bottom_row:
                self.print_card(card)

    def print_card(self, card):
        if(type(card) == int):
            print("no card")
        else:    
            print("card: " + card.rank + " " +card.suit)
            #print("rank: " + card.rank)
    
        
        
        
                    


#break this out into separate test file        

print("STARTING GAME")

game = Game()

print("ADDING PLAYERS")
game.add_player("Joe")
game.add_player("Steve")

print("DEALING CARDS")
game.deal()


game.print_board()
        
                
game.play_card(1, 1, 1)

game.print_board()

game.play_card(2, 2, 2)


#def deal(hands deck)



#Each player has a hand and a board
#class Player:

    
# print(type(Suit.HEART.name))

# print(list(Suit))

# print(list(Rank))

# woof = Card(Suit.HEART, Rank.ACE)

# print("woof" + woof.rank)

# print("woof2" + woof.suit)

# woofwoof = Deck()

# woofwoof.shuffle()

# for card in woofwoof.cards:
#     print("suit " + card.suit + " rank " + card.rank)

#print(list(woofwoof.cards))