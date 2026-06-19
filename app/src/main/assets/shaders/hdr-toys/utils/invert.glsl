// invert the signal

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC signal invert

float invert(float x, float w) {
    return -x + w;
}

vec3 invert(vec3 x, float w) {
    return -x + w;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = invert(color.rgb, 1.0);

    return color;
}
