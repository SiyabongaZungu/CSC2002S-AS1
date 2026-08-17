import java.util.concurrent.ForkJoinPool;

public class FirelineParallel {

    private static final int DEFAULT_MAXIMUM_STEPS = 5000;
    private static final double DEFAULT_TOLERANCE = 0.05;

    private static final int CUTOFF = 1000;

    public static void main(String[] args) {

        if (args.length < 5 || args.length > 11|| (args.length > 8 && args.length < 11)) {
            printUsage();
            return;
        }

        try {

            int rows = Integer.parseInt(args[0]);
            int columns = Integer.parseInt(args[1]);
            long seed = Long.parseLong(args[2]);

            FireMap.Mode mode = FireMap.Mode.fromString(args[3]);

            String outputPrefix = args[4];

            int maximumSteps = DEFAULT_MAXIMUM_STEPS;

            if (args.length >= 6) {
               maximumSteps = Integer.parseInt(args[5]);
            }

            double tolerance = DEFAULT_TOLERANCE;

            if (args.length >= 7) {
                tolerance = Double.parseDouble(args[6]);
            }

            FireMap.Landscape landscape = FireMap.Landscape.MIXED;

            if (args.length >= 8) {
                landscape = FireMap.Landscape.fromString(args[7]);
            }

           //  Integer ignitionTopRow = null;
//             Integer ignitionLeftColumn = null;
//             Integer ignitionPatchSize = null;
// 
//             if (args.length == 11) {
// 
//                 ignitionTopRow = Integer.parseInt(args[8]);
// 
//                 ignitionLeftColumn = Integer.parseInt(args[9]);
// 
//                 ignitionPatchSize = Integer.parseInt(args[10]);
//             }

            FireMap map = new FireMap( rows, columns, seed, mode, landscape, ignitionTopRow,ignitionLeftColumn,ignitionPatchSize);

            ForkJoinPool pool = new ForkJoinPool();

            FireMap.StepResult result = null;

            int stepsCompleted = 0;
            boolean converged = false;
            long startTime = System.nanoTime();

            while (stepsCompleted < maximumSteps) {
                map.prepareNextState();
                FireTask task = new FireTask( map,mode,1, rows - 1, 1, columns - 1,CUTOFF);
                
                pool.execute(task);

                result = task.join();
                
                map.completeStep();

                stepsCompleted++;

                if (mode == FireMap.Mode.WILDFIRE) {

                    converged = result.getBurningCells() == 0 && result.getMaximumTemperatureChange()< tolerance;

                } else {

                    converged = result.getMaximumTemperatureChange()< tolerance;
                }

                if (converged) {
                    break;
                }
            }

            long endTime = System.nanoTime();

            double elapsedMilliseconds = (endTime - startTime) / 1_000_000.0;


            map.writeImages(outputPrefix);

  
            System.out.println("Fireline serial simulation");
            System.out.printf("Mode: %s%n", mode.name().toLowerCase());
            System.out.printf("Rows: %d%n", rows);
            System.out.printf("Columns: %d%n", columns);
            System.out.printf("Random seed: %d%n", seed);
            System.out.printf("Landscape: %s%n",landscape.name().toLowerCase());
            System.out.printf("Initial source: %s%n",map.getSourceDescription());
            System.out.printf("Timesteps completed: %d%n", stepsCompleted);
            System.out.printf("Converged: %s%n", converged ? "yes" : "no");
            System.out.printf("Final burning cells: %d%n",result == null? 0: result.getBurningCells());

            System.out.printf( "Cells burned: %d%n", map.countBurnedCells());

            System.out.printf("Maximum peak temperature: %.3f%n", map.getMaximumPeakTemperature());

            System.out.printf("Maximum change in final timestep: %.6f%n", result == null? 0.0 : result.getMaximumTemperatureChange());

            System.out.printf("Core simulation time: %.3f ms%n", elapsedMilliseconds);

            System.out.printf("Images written with prefix: %s%n", outputPrefix);

            if (!converged) {
                System.out.println("Warning: maximum timestep limit reached " + "before convergence.");
            }

            pool.shutdown();

        } catch (Exception e) {

            System.err.println(
                    "Simulation failed: " + e.getMessage());

            e.printStackTrace();
        }
    }

    private static void printUsage() {

        System.err.println("Usage: java FirelineParallel <rows> <columns> "+ "<seed> <diffusion|wildfire> <output-prefix> " + "[max-steps] [tolerance] [mixed|grass] " + "[ignition-top-row ignition-left-column patch-size]");
    }
}