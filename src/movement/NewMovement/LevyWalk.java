package movement.NewMovement;

import core.Coord;
import core.Settings;
import movement.MovementModel;
import movement.Path;

/**
 * @see <a href="https://en.wikipedia.org/wiki/L%C3%A9vy_flight">Lévy flight</a>
 * @see <a href="https://ieeexplore.ieee.org/document/5750071">On the Levy-Walk Nature of Human Mobility</a>
 * 
 * 
 * Kelas ini mengimplementasikan model pergerakan Levy Walk
 * Model ini digunakan untuk mensimulasikan pergerakan individu dalam suatu area
 * dengan pola yang mengikuti distribusi Levy.
 * 
 *  * Levy walk movement, adapted from Lévy flight model.
 *
 * Model ini menggunakan distribusi Pareto untuk menentukan panjang langkah (step length)
 * dan arah acak untuk simulasi mobilitas realistis (misalnya manusia/hewan).
 * 
 * Model ini juga dapat digunakan untuk mensimulasikan pergerakan acak dengan
 * parameter yang sesuai.
 * @author [Your Name]
 */
public class LevyWalk extends MovementModel{

    //kunci untuk membaca parameter dari file konfigurasi (Settings)
    private static final String ALPHA_S = "alpha";
    private static final String MIU_S = "miu";
    private static final String STEPSRANGE_S = "stepsRange";

    //Parameter Model Levy Walk
    private double alpha; // Parameter untuk distribusi Pareto (kemiringan)
    private double beta;
    private double miu; // Waktu jeda (pause) antar langkah (belum digunakan)
    private int[] stepsRange; // Rentang jumlah langkah

    //Lokasi saat ini
    protected Coord currentLocation;

    /**
     * Konstruktor utama yang mengambil Nilai dari konfigurasi untuk model Levy Walk
     * @param settings
     */
    public LevyWalk(Settings settings) {
        super(settings);

        // Mengambil nilai parameter dari konfigurasi, default 3.0 jika tidak ada
        this.alpha = settings.contains(ALPHA_S)? settings.getDouble(ALPHA_S) : 3.0f;

        // Mengambil nilai parameter dari konfigurasi, default 1.0 jika tidak ada
        this.miu = settings.contains(MIU_S)? settings.getDouble(MIU_S) : 1.0f;

        // Mengambil nilai parameter dari rentang langkah
        this.stepsRange = settings.getCsvInts(STEPSRANGE_S);

        this.currentLocation = randomCoord();
    }

    /**
     * Konstruktor duplikasi untuk melakukan kloning objek LevyWalk
     * @param original
     */

    public LevyWalk(LevyWalk original) {
        super(original);
        this.alpha = original.alpha;
        this.miu = original.miu;
        this.stepsRange = original.stepsRange;
        this.currentLocation = randomCoord(); //lokasi baru
    }

    /**
     * Menghasilkan lintasan (path) dari pergerakan berdasarkan Levy Walk.
     */
    @Override
    public Path getPath(){
        final int steps = rng.nextInt(stepsRange[0], stepsRange[1]); // jumlah langkah acak
        final Path path = new Path(generateSpeed()); // buat path baru dengan kecepatan acak
        for (int i = 0; i < steps; i++) {
            int nextX, nextY;

            do { 
                // Panjang langkah dari distribusi Pareto
                int step_length = nextPareto(alpha, 3);  // xm = 3

                //Ambil arah acak dari (theta) antara 0 dan 2π
                double theta = rng.nextDouble(0,2 *  Math.PI);

                //Hitung koordinat tujuan berdasarkan arah dan panjang langkah 
                nextX = (int) (currentLocation.getX() + step_length * Math.cos(theta));
                nextY = (int) (currentLocation.getY() + step_length * Math.sin(theta));
                
            } while (nextX >= getMaxX() || nextY >= getMaxY() || nextX <= 0 || nextY <= 0); // pastikan tidak keluar dari batas

            //Tambahkan koordinat tujuan ke path 
            Coord nextCoord = new Coord(nextX, nextY);
            path.addWaypoint(nextCoord);

            //Update lokasi saat ini
            currentLocation = nextCoord;
        }
        return path;
    }

    /**
     * Menghasilkan lokasi awal secara acak di dalam area.
     */
    @Override
    public Coord getInitialLocation() {
        //assert rng != null : "RNG not initialized";
        // Menghasilkan koordinat acak dalam batas area
        return randomCoord();
    }

    /**
     * Menghasilkan salinan (replica) dari objek ini.
     */
    @Override
    public LevyWalk replicate() {
        return new LevyWalk(this);
    }

    /**
     * menghasilkan angka acak berdasarkanh distribusi Pareto
     * 
     * @param alpha parameter distribusi Pareto, Kemiringan distribusi (semakin kecil, langkah semakin panjang)
     * @param xm nilai minimum untuk distribusi Pareto
     * @return angka acak dari distribusi Pareto
     */
    // private int nextPareto(double alpha, double xm) {
    //     double uniformRandom = rng.nextDouble(); // acak antara 0 dan 1
    //     return (int) (xm / Math.pow(1.0 - uniformRandom, 1.0 / alpha));
    // }
    private int nextPareto(double alpha, double xm) {
        assert rng != null : "RNG not initialized";
        // Menghasilkan angka acak dari distribusi Pareto
        double u = rng.nextDouble(0, 1);
        return (int) (xm / Math.pow(u, 1.0 / (alpha - 1)));
    }

    /**
     * Menghasilkan koordinat acak dalam batas area
     * @return koordinat acak
     */
    protected Coord randomCoord() {
        return new Coord(rng.nextDouble() * getMaxX(), rng.nextDouble() * getMaxY());
    }


}
