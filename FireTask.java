import java.util.concurrent.RecursiveTask;

public class FireTask extends RecursiveTask<FireMap.StepResult> {

    private FireMap map;
    private FireMap.Mode mode;

    private int rowStart;
    private int rowEnd;
    private int colStart;
    private int colEnd;

    private int CUTOFF;

    public FireTask(FireMap map, FireMap.Mode mode, int rowStart, int rowEnd, int columnStart, int columnEnd, int cutoff) {
        this.map = map;
        this.mode = mode;
        columnStart = colStart;
        columnEnd = colEnd;
        cutoff = CUTOFF;
    }

    @Override
    protected FireMap.StepResult compute() {

        int numberOfRows = rowEnd - rowStart;
        int numberOfColumns = colEnd - colStart;

        //check if small enough
        if (numberOfRows * numberOfColumns <= CUTOFF) {

            return map.updateRegion(mode, rowStart, rowEnd, colStart, colEnd);
        }


        if (numberOfRows >= numberOfColumns) {

            int middle = (rowStart + rowEnd) / 2;

            FireTask first = new FireTask(map, mode, rowStart, middle, colStart, colEnd, CUTOFF);

            FireTask second = new FireTask( map, mode, middle, rowEnd, colStart, colEnd, CUTOFF);


            first.fork();
            
            FireMap.StepResult secondResult = second.compute();

            FireMap.StepResult firstResult = first.join();

            // Combine the results.
            return FireMap.StepResult.combine(firstResult, secondResult);

        } else {

            int middle =
                    (colStart + colEnd) / 2;

            FireTask first = new FireTask(map, mode, rowStart, rowEnd, colStart, middle, CUTOFF);

            FireTask second = new FireTask(map,mode,rowStart, rowEnd, middle,colEnd, CUTOFF);

            first.fork();

            FireMap.StepResult secondResult = second.compute();

            FireMap.StepResult firstResult = first.join();

            return FireMap.StepResult.combine(firstResult, secondResult);
        }
    }
}