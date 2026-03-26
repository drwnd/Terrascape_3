#version 400 core
#define MAX_TEXT_LENGTH 128

in int data;

out vec2 textureCoordinates;

uniform vec2 charSize;
uniform int[MAX_TEXT_LENGTH] string;
uniform float[MAX_TEXT_LENGTH + 1] offsets;
uniform vec2 position;

void main() {
    int index = data & MAX_TEXT_LENGTH - 1;
    float charPixelWidth = offsets[index + 1] - offsets[index];

    float deltaX = (data >> 8 & 1) * charPixelWidth;
    float deltaY = data >> 9 & 1;

    float x = 2 * (position.x + (offsets[index] + deltaX) * charSize.x);
    float y = 2 * (position.y + deltaY * charSize.y);
    gl_Position = vec4(x, y, 1, 1);

    int character = string[index];
    float u = ((character & 15) + deltaX) * 0.0625;
    float v = ((character >> 4 & 15) + 1 - deltaY) * 0.0625;
    textureCoordinates = vec2(u, v);
}