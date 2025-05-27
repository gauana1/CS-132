package utils;

import java.util.HashSet;
import java.util.Set;

public class LivenessObject {
    public Set<String> def = new HashSet<>();
    public Set<String> use = new HashSet<>();

    public Set<String> sucessors = new HashSet<>();

    public Set<String> prevIn = new HashSet<>();
    public Set<String> prevOut = new HashSet<>();

    public Set<String> currIn = new HashSet<>();
    public Set<String> currOut = new HashSet<>();

    public Boolean isLabel = false;
}
