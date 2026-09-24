        if (text == null) {
            return 0;
        }
        Map<Character, Integer> lastSeen = new HashMap<>();
        int best = 0;
        int left = 0;
        for (int right = 0; right < text.length(); right++) {
            Integer previous = lastSeen.put(text.charAt(right), right);
            if (previous != null && previous >= left) {
                left = previous + 1;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
