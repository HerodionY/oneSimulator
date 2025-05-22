package movement;

import java.util.Random;

import core.Coord;
import core.Settings;
import static movement.MovementModelE.rng;
public class RandomArea extends MovementModelE {

     /**
    * area lokasi node -setting id ({@value})
    */
    public static final String MOVE_AREA = "moveArea";
    /**
     * how many waypoints should there be per path
     */
    private static final int PATH_LENGTH = 1;
    
    private Coord lastWaypoint;

    public RandomArea(Settings settings) {
        super(settings);
    }

    protected RandomArea(RandomArea rwp) {
        super(rwp);
    }

    /**
     * Returns a possible (random) placement for a host
     *
     * @return Random position on the map
     */
    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = randomCoord(moveArea);

        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());
        Coord c = lastWaypoint;

        for (int i = 0; i < PATH_LENGTH; i++) {
            c = randomCoord(moveArea);
            p.addWaypoint(c);
        }

        this.lastWaypoint = c;
        return p;
    }
    

   
    @Override
    public RandomArea replicate(){
        return new RandomArea(this);
    }
    

    protected Coord randomCoord() {
        return new Coord(rng.nextDouble() * getMaxX(),
                rng.nextDouble() * getMaxY());
    }

    /**
     * Bangkitkan koordinat berdasarkan area
     * @param area area node berada
     * @return koordinat kemana node akan jalan selanjutnya
     */
    protected Coord randomCoord(int area) {
        double minX, maxX, minY, maxY;

        switch (area) {
            case 1:             // area 1 miner
                minX = 0;
                maxX = 333;
                minY = 0;
                maxY = 333;
                break;
            case 2:             // area 2 miner
                minX = 333;
                maxX = 666;
                minY = 0;
                maxY = 333;
                break;
            case 3:             // area 3 miner
                minX = 666;
                maxX = 1000;
                minY = 0;
                maxY = 333;
                break;
            case 4:             // area 4 miner
                minX = 0;
                maxX = 333;
                minY = 333;
                maxY = 666;
                break;
            case 5:             // area 5 miner
                minX = 666;
                maxX = 1000;
                minY = 333;
                maxY = 666;
                break;
            case 6:             // area 6 miner
                minX = 0;
                maxX = 333;
                minY = 666;
                maxY = 1000;
                break;
            case 7:             // area 7 miner
                minX = 333;
                maxX = 666;
                minY = 666;
                maxY = 1000;
                break;
            case 8:             // area 8 miner
                minX = 666;
                maxX = 1000;
                minY = 666;
                maxY = 1000;
                break;
            case 9:
                minX = 333;
                maxX = 666;  
                minY = 333;
                maxY = 1000;  
                break;
            case 10:            // area 1 operator proxy
                minX = 0;
                maxX = 500;
                minY = 0;
                maxY = 500;
                break;
            case 11:            // area 2 operator proxy
                minX = 500;
                maxX = 500;
                minY = 0;
                maxY = 500;
                break;
            case 12:            // area 3 operator proxy
                minX = 500;
                maxX = 1000;
                minY = 0;
                maxY = 500;
                break;
            case 13:            // area 4 operator proxy
                minX = 0;
                maxX = 500;
                minY = 500;
                maxY = 500;
                break;
            case 14:            // area 5 operator proxy
                minX = 500;
                maxX = 1000;
                minY = 500;
                maxY = 500;
                break;
            case 15:            // area 6 operator proxy
                minX = 0;
                maxX = 500;
                minY = 500;
                maxY = 1000;
                break;
            case 16:            // area 7 operator proxy
                minX = 500;
                maxX = 500;
                minY = 500;
                maxY = 1000;
                break;
            case 17:            // area 8 operator proxy
                minX = 500;
                maxX = 1000;
                minY = 500;
                maxY = 1000;
                break;
            case 18 :           // collector
                minX = 500;
                maxX = 1200;
                minY = 500;
                maxY = 500;
                break;
            default:
                throw new IllegalArgumentException("Area harus antara 1-4 atau 6-9 (area 5 tidak bisa dipilih)");
        }
        
        double x, y;
        
        // setting tambahan case pergerakan diagonal
        switch (area) {
            
            // area 1 dan area 8 (diagonal menurun)
            case 10:
            case 17:
                x = minX + rng.nextDouble() * (maxX - minX);
                y = x;
                break;
                
            // area 3 (diagonal menaik)
            case 12:
                x = minX + rng.nextDouble() * (maxX - minX);
                y = 1000 - x;
                break;
                
            // area 6 (diagonal menaik)
            case 15:
                y = minY + rng.nextDouble() * (maxY - minY);
                x = 1000 - y;
                break;
                
            // area 2, 4, 5, dan 7 default (horizontal & vertikal)
            default:
                x = minX + rng.nextDouble() * (maxX - minX);
                y = minY + rng.nextDouble() * (maxY - minY);
                break;
        }

        return new Coord(x, y);
    }

  

    
}
