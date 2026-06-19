// https://lut-to-texture.pages.dev/
// You can convert .cube format 3D LUTs to the desired texture format using the link above,
// then paste it to the last line.

//!HOOK OUTPUT
//!BIND HOOKED
//!BIND LUT
//!DESC LUT

vec3 tetrahedral(sampler3D lut, vec3 color) {
    float lut_size = float(textureSize(lut, 0).x);
    vec3 coord = color * (lut_size - 1.0);

    vec3 b = floor(coord);
    vec3 f = fract(coord);

    float texel_size = 1.0 / lut_size;
    vec3 base_coord = (b + 0.5) * texel_size;

    vec3 c000 = base_coord;
    vec3 c100 = base_coord + vec3(texel_size, 0.0, 0.0);
    vec3 c010 = base_coord + vec3(0.0, texel_size, 0.0);
    vec3 c110 = base_coord + vec3(texel_size, texel_size, 0.0);
    vec3 c001 = base_coord + vec3(0.0, 0.0, texel_size);
    vec3 c101 = base_coord + vec3(texel_size, 0.0, texel_size);
    vec3 c011 = base_coord + vec3(0.0, texel_size, texel_size);
    vec3 c111 = base_coord + vec3(texel_size, texel_size, texel_size);

    vec3 v000 = texture(lut, c000).rgb;
    vec3 v100 = texture(lut, c100).rgb;
    vec3 v010 = texture(lut, c010).rgb;
    vec3 v110 = texture(lut, c110).rgb;
    vec3 v001 = texture(lut, c001).rgb;
    vec3 v101 = texture(lut, c101).rgb;
    vec3 v011 = texture(lut, c011).rgb;
    vec3 v111 = texture(lut, c111).rgb;

    vec3 result;
    if (f.x >= f.y && f.y >= f.z) {
        // Tetrahedron 1: x >= y >= z
        result = (1.0 - f.x) * v000 +
                 (f.x - f.y) * v100 +
                 (f.y - f.z) * v110 +
                 f.z * v111;
    } else if (f.x >= f.z && f.z >= f.y) {
        // Tetrahedron 2: x >= z >= y
        result = (1.0 - f.x) * v000 +
                 (f.x - f.z) * v100 +
                 (f.z - f.y) * v101 +
                 f.y * v111;
    } else if (f.y >= f.x && f.x >= f.z) {
        // Tetrahedron 3: y >= x >= z
        result = (1.0 - f.y) * v000 +
                 (f.y - f.x) * v010 +
                 (f.x - f.z) * v110 +
                 f.z * v111;
    } else if (f.y >= f.z && f.z >= f.x) {
        // Tetrahedron 4: y >= z >= x
        result = (1.0 - f.y) * v000 +
                 (f.y - f.z) * v010 +
                 (f.z - f.x) * v011 +
                 f.x * v111;
    } else if (f.z >= f.x && f.x >= f.y) {
        // Tetrahedron 5: z >= x >= y
        result = (1.0 - f.z) * v000 +
                 (f.z - f.x) * v001 +
                 (f.x - f.y) * v101 +
                 f.y * v111;
    } else {
        // Tetrahedron 6: z >= y >= x
        result = (1.0 - f.z) * v000 +
                 (f.z - f.y) * v001 +
                 (f.y - f.x) * v011 +
                 f.x * v111;
    }

    return result;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);
    color.rgb = tetrahedral(LUT, color.rgb);
    return color;
}

//!TEXTURE LUT
//!SIZE 65 65 65
//!FORMAT rgba16hf
//!FILTER NEAREST
