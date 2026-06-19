// https://en.wikipedia.org/wiki/Exposure_value

//!PARAM exposure_value
//!TYPE float
//!MINIMUM -64
//!MAXIMUM  64
0.0

//!HOOK OUTPUT
//!BIND HOOKED
//!WHEN exposure_value
//!DESC exposure

float exposure(float x, float ev) {
    return x * exp2(ev);
}

vec3 exposure(vec3 x, float ev) {
    return x * exp2(ev);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = exposure(color.rgb, exposure_value);

    return color;
}
