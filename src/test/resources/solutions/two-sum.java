        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < numbers.length; i++) {
            Integer partner = seen.get(target - numbers[i]);
            if (partner != null) {
                return new int[] {partner, i};
            }
            seen.put(numbers[i], i);
        }
        return new int[0];
