        int low = 0;
        int high = numbers.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (numbers[middle] == target) {
                return middle;
            } else if (numbers[middle] < target) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return -1;
