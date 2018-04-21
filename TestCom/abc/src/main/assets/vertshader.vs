attribute  	vec3 position;
varying 	vec3 xyz;

uniform mat3  _viewmat;
uniform float _coshfov;
uniform float _cschfov;
uniform float _d;
uniform vec2  _size;

void main()
{
   vec3 tmp 	= _viewmat * position;
   
   float coef 	= _cschfov * ( (_coshfov + _d) / (_d - tmp.z));
   float u  	= tmp.x * coef;
   float v  	= tmp.y * coef;
   float z  	= tmp.z;

    if (_size.x > _size.y)
   {
    	v = v * _size.x / _size.y;
   }
   else
   {
		u = u * _size.y / _size.x;
   }

   gl_Position = vec4(u, v, z, 1.0);
   xyz = position;
}