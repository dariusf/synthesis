package synthesis;

class SynthesisData {
    String title;
    String text;
    int type;
    
    public SynthesisData(String title, String text, int type) {
        this.title = title;
        this.text = text;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return title;
    }
}
