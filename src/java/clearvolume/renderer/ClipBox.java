package clearvolume.renderer;

public class ClipBox
{
	public float xmin,xmax, ymin, ymax, zmin,zmax;

	public ClipBox(	float pXmin,
									float pXmax,
									float pYmin,
									float pYmax,
									float pZmin,
									float pZmax)
	{
		super();
		xmin = pXmin;
		xmax = pXmax;
		ymin = pYmin;
		ymax = pYmax;
		zmin = pZmin;
		zmax = pZmax;
	}

	public float[] getArray()
	{
		return new float[]{xmin,xmax, ymin, ymax, zmin,zmax};
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(xmax);
		result = prime * result + Float.floatToIntBits(xmin);
		result = prime * result + Float.floatToIntBits(ymax);
		result = prime * result + Float.floatToIntBits(ymin);
		result = prime * result + Float.floatToIntBits(zmax);
		result = prime * result + Float.floatToIntBits(zmin);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClipBox other = (ClipBox) obj;
		if (Float.floatToIntBits(xmax) != Float.floatToIntBits(other.xmax))
			return false;
		if (Float.floatToIntBits(xmin) != Float.floatToIntBits(other.xmin))
			return false;
		if (Float.floatToIntBits(ymax) != Float.floatToIntBits(other.ymax))
			return false;
		if (Float.floatToIntBits(ymin) != Float.floatToIntBits(other.ymin))
			return false;
		if (Float.floatToIntBits(zmax) != Float.floatToIntBits(other.zmax))
			return false;
		if (Float.floatToIntBits(zmin) != Float.floatToIntBits(other.zmin))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "ClipBox [xmin=" + xmin
						+ ", xmax="
						+ xmax
						+ ", ymin="
						+ ymin
						+ ", ymax="
						+ ymax
						+ ", zmin="
						+ zmin
						+ ", zmax="
						+ zmax
						+ "]";
	}
}
