package arina.q.camel.jmx;

public class Stat
{
    long start = -1;
    long finish = -1;

    public static Stat start()
    {
        Stat d = new Stat();
        d.start = System.currentTimeMillis();
        return d;
    }

    public Stat finish()
    {
        if(this.start > 0)
            this.finish = System.currentTimeMillis();

        return this;
    }

    public long getDuration()
    {
        if(this.finish > 0 && this.start > 0)
            return this.finish - this.start;
        else
            return 0;
    }

    public long getFinish()
    {
        return this.finish;
    }
}
