package com.neoassetextractor.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object containing extraction statistics and messages
 */
public class ExtractionResult {
    private boolean success;
    private int blockstatesExtracted;
    private int modelsExtracted;
    private int texturesExtracted;
    private final List<String> messages;
    private final List<String> warnings;
    private final List<String> errors;
    
    public ExtractionResult() {
        this.messages = new ArrayList<String>();
        this.warnings = new ArrayList<String>();
        this.errors = new ArrayList<String>();
    }
    
    public void addMessage(String message) { messages.add(message); }
    public void addWarning(String warning) { warnings.add(warning); }
    public void addError(String error) { errors.add(error); }
    
    public void incrementBlockstates() { blockstatesExtracted++; }
    public void incrementModels() { modelsExtracted++; }
    public void incrementTextures() { texturesExtracted++; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public int getBlockstatesExtracted() { return blockstatesExtracted; }
    public int getModelsExtracted() { return modelsExtracted; }
    public int getTexturesExtracted() { return texturesExtracted; }
    
    public List<String> getMessages() { return messages; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }
    
    public int getTotalExtracted() {
        return blockstatesExtracted + modelsExtracted + texturesExtracted;
    }
    
    public String getSummary() {
        return String.format("Blockstates: %d, Models: %d, Textures: %d", 
            blockstatesExtracted, modelsExtracted, texturesExtracted);
    }
}
