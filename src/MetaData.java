class MetaData {
    public int popularity;
    public String id;

    public MetaData(String id, int popularity) {
        this.id = id;
        this.popularity = popularity;
    }

    @Override
    public String toString() {
        return "[ " + "Id = " + id + ", popularity = " + popularity + " ]";
    }

}