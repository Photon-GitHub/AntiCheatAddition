package de.photon.AACAdditionPro.util.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Trie} datastructure.
 * This {@link Trie} ignores upper / lower cases and only supports the english alphabet.
 */
public class Trie
{
    private TrieNode root;

    public Trie()
    {
        root = new TrieNode();
    }

    /**
     * Adds a word to the {@link Trie}
     *
     * @param word the word that should be added to the {@link Trie}
     */
    public void addWord(final String word)
    {
        root.addWord(word.toLowerCase());
    }

    /**
     * Get the words in the {@link Trie} with the given prefix
     *
     * @param prefix        the characters that every returned word should start with
     * @param prefixRemoved whether or not the prefix should be included in the final result.
     *
     * @return a List containing String objects containing the words in
     * the Trie with the given prefix.
     */
    public List<String> getWords(final String prefix, final boolean prefixRemoved)
    {
        // Find the node which represents the last letter of the prefix
        TrieNode lastNode = root;
        for (char c : prefix.toLowerCase().toCharArray())
        {
            lastNode = lastNode.getNode(c);

            // If no node matches, then no words exist, return empty list
            if (lastNode == null)
                return Collections.emptyList();
        }

        //Return the words which eminate from the last node
        return prefixRemoved ? lastNode.getWordsPrefixRemoved(0) : lastNode.getWords();
    }

    private class TrieNode
    {
        private TrieNode parent;
        private TrieNode[] children;
        private boolean isLeaf;     //Quick way to check if any children exist
        private boolean isWord;     //Does this node represent the last character of a word
        private char character;     //The character this node represents

        /**
         * Constructor for top level root node.
         */
        private TrieNode()
        {
            children = new TrieNode[26];
            isLeaf = true;
            isWord = false;
        }

        /**
         * Constructor for child node.
         */
        private TrieNode(char character)
        {
            this();
            this.character = character;
        }

        /**
         * Adds a word to this node. This method is called recursively and
         * adds child nodes for each successive letter in the word, therefore
         * recursive calls will be made with partial words.
         *
         * @param word the word to add
         */
        private void addWord(String word)
        {
            isLeaf = false;
            int charPos = word.charAt(0) - 'a';

            if (children[charPos] == null)
            {
                children[charPos] = new TrieNode(word.charAt(0));
                children[charPos].parent = this;
            }

            if (word.length() > 1)
            {
                children[charPos].addWord(word.substring(1));
            }
            else
            {
                children[charPos].isWord = true;
            }
        }

        /**
         * @param c the char which {@link TrieNode} should be found.
         *
         * @return the child TrieNode representing the given char,
         * * or null if no node exists.
         */
        private TrieNode getNode(char c)
        {
            return children[c - 'a'];
        }

        /**
         * @return a List of String objects which are lower in the
         * hierarchy that this node.
         */
        private List<String> getWords()
        {
            //Create a list to return
            final List<String> list = new ArrayList<>();

            //If this node represents a word, add it
            if (isWord)
            {
                list.add(this.toString());
            }

            //If any children
            if (!isLeaf)
            {
                // Add any words belonging to any children
                for (TrieNode child : children)
                {
                    if (child != null)
                    {
                        list.addAll(child.getWords());
                    }
                }
            }
            return list;
        }

        private List<String> getWordsPrefixRemoved(int depth)
        {
            //Create a list to return
            final List<String> list = new ArrayList<>();

            //If this node represents a word, add it
            if (isWord)
            {
                list.add(this.toString(depth));
            }

            //If any children
            if (!isLeaf)
            {
                // Add any words belonging to any children
                for (TrieNode child : children)
                {
                    if (child != null)
                    {
                        list.addAll(child.getWordsPrefixRemoved(depth + 1));
                    }
                }
            }
            return list;
        }

        /**
         * Gets the String that this node represents.
         * <p>
         * For example, if this node represents the character t, whose parent
         * <p>
         * represents the charater a, whose parent represents the character
         * <p>
         * c, then the String would be "cat".
         */
        public String toString()
        {
            return parent == null ? "" : parent.toString() + character;
        }

        /**
         * Gets the String that this node represents with a limitation to the parent nodes.
         */
        public String toString(int depth)
        {
            return parent != null && depth > 0 ?
                   parent.toString(depth - 1) + character :
                   String.valueOf(character);
        }
    }
}