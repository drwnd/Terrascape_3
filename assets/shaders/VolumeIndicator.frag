#version 400 core

out vec4 fragColor;
in vec3 totalPosition;
flat in int side;

uniform sampler2DArray textures;
uniform int material;

vec2 getUVOffset(int side) {
    switch (side) {
        case 0: return vec2(fract(totalPosition.x * 0.0625), 1 - fract(totalPosition.y * 0.0625));
        case 1: return fract(totalPosition.xz * 0.0625);
        case 2: return 1 - fract(totalPosition.zy * 0.0625);
        case 3: return 1 - fract(totalPosition.xy * 0.0625);
        case 4: return fract(totalPosition.xz * 0.0625);
        case 5: return vec2(fract(totalPosition.z * 0.0625), 1 - fract(totalPosition.y * 0.0625));
    }

    return fract(totalPosition.zx);
}

void main() {
    vec4 color;
    if (material != 0) {
        vec3 textureCoord = vec3(getUVOffset(side), material & 0xFF);
        color = texture(textures, textureCoord);
    } else {
        float sum = floor(totalPosition.x + 0.5) + floor(totalPosition.y + 0.5) + floor(totalPosition.z + 0.5);
        color = vec4(sin(sum * 6.283185307 * 0.125), 0, 0, 0.5);
    }
    fragColor = vec4(color.rgb, color.a * 0.5);
}