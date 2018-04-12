package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;

public class Game extends Pane {

    private List<Card> deck;
    private List<Card> tempdeck;

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        if (!card.isFaceDown()) {
            Pile activePile = card.getContainingPile();
            if (activePile.getPileType() == Pile.PileType.STOCK)
                return;
            double offsetX = e.getSceneX() - dragStartX;
            double offsetY = e.getSceneY() - dragStartY;

            draggedCards.clear();
            draggedCards.add(card);

            card.getDropShadow().setRadius(20);
            card.getDropShadow().setOffsetX(10);
            card.getDropShadow().setOffsetY(10);

            card.toFront();
            card.setTranslateX(offsetX);
            card.setTranslateY(offsetY);
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards == null || draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pileTableau = getValidIntersectingPile(card, tableauPiles);
        Pile pileFoundation = getValidIntersectingPile(card, foundationPiles);

        if (pileTableau != null) {
            handleValidMove(card, pileTableau);
        } else if (pileFoundation != null) {
            handleValidMove(card, pileFoundation);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };


    public boolean isGameWon() {
        for (Pile pile : foundationPiles) {
            if (pile.getCards().size() != 13) {
                return false;
            }
        }
        return true;
    }


    public Game() {
        deck = Card.createNewDeck();
        tempdeck = new ArrayList(deck);
        Collections.shuffle(deck);
        initPiles();
        dealCards();
        initButtons();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        List<Card> discardPileCards =discardPile.getCards();
        stockPile.clear();
        stockPile.addCards(discardPileCards);
        discardPile.clear();

        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {

        int sourceSuit = card.getSuit();
        int sourceVal = card.getRank();
        Card destination = destPile.getTopCard();
        //if destination pile is empty
        if (destination == null){
            switch (destPile.getPileType())
            {
                case FOUNDATION:
                    if (sourceVal == 1){
                        return true;
                    }
                    break;
                case TABLEAU:
                    if (sourceVal == 13){
                        return true;
                    }
                    break;
            }
            return false;
        }

        //if the pile is not empty
        int destinationVal = destination.getRank();
        int destinationSuit = destination.getSuit();

        //destination is foundation
        if (destPile.getPileType() == Pile.PileType.FOUNDATION){
            if ((sourceVal - 1) == destinationVal){
                if(sourceSuit == destinationSuit){
                    return true;
                }
            }
            return false;
        }
        
        //destination is tableau
        if ((sourceVal + 1) == destinationVal) {
            switch (sourceSuit) {
                case 1:
                    if (destinationSuit == 3 || destinationSuit == 4)
                        return true;
                    break;
                case 2:
                    if (destinationSuit == 3 || destinationSuit == 4)
                        return true;
                    break;
                case 3:
                    if (destinationSuit == 1 || destinationSuit == 2)
                        return true;
                    break;
                case 4:
                    if (destinationSuit == 1 || destinationSuit == 2)
                        return true;
                    break;
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
            foundationPile.getCards().addListener((ListChangeListener<Card>) c -> {
                if (isGameWon()) {
                    System.out.println("YOU WONNED");
                }
            });
        }

        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();

        int cardsToBePlaced = 1;
        for ( Pile tableauPile : tableauPiles) {
            for ( int i = 0; i < cardsToBePlaced; i++) {
                Card currentCard = deckIterator.next();
                tableauPile.addCard(currentCard);
                addMouseEventHandlers(currentCard);
                getChildren().add(currentCard);
            }
            Card lastCardOnPile = tableauPile.getTopCard();
            if (lastCardOnPile.isFaceDown()){
                lastCardOnPile.flip();
            }
            cardsToBePlaced ++;
            tableauPile.attachFlipHandler();
        }

        deckIterator.forEachRemaining(card -> {
            if(!card.isFaceDown()){
                card.flip();
            }
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public void initButtons() {
        Button restartBtn = new Button("Restart");
        restartBtn.setLayoutY(25);
        restartBtn.setLayoutX(480);
        restartBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("event handler");
                restartGame();
            }
        });
        getChildren().add(restartBtn);

        Button newGameBtn = new Button("New Game");
        newGameBtn.setLayoutY(80);
        newGameBtn.setLayoutX(480);
        newGameBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                newGame();
            }
        });
        getChildren().add(newGameBtn);
    }
    public void restartGame() {
        clearAllPiles();
        initPiles();
        dealCards();
        initButtons();
    }

    public void newGame() {
        clearAllPiles();
        deck = Card.createNewDeck();
        Collections.shuffle(deck);
        initPiles();
        dealCards();
        initButtons();
    }

    public void clearAllPiles() {
        getChildren().clear();
        tableauPiles.clear();
        foundationPiles.clear();
        stockPile.clear();
        discardPile.clear();
    }


}
