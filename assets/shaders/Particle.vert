#version 460 core

out vec3 totalPosition;
flat out vec3 normal;
flat out int textureData;

struct Particle {
    int packedOffset;
    int packedVelocityGravity;
    int packedLifeTimeRotationMaterial;
};

layout (std430, binding = 0) restrict readonly buffer particleBuffer {
    Particle[] particles;
};

uniform mat4 projectionViewMatrix;
uniform int currentTime;
uniform int spawnTime;
uniform ivec3 iCameraPosition;
uniform ivec3 startPosition;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec2[6] FACE_POSITIONS = vec2[6](vec2(0, 0), vec2(0, 1), vec2(1, 0), vec2(1, 1), vec2(1, 0), vec2(0, 1));

const float VELOCITY_PACKING_FACTOR = 0.25;     // Inverse in Particle.java
const float GRAVITY_PACKING_FACTOR = 0.5;       // Inverse in Particle.java
const float ROTATION_PACKING_FACTOR = 0.0625;   // Inverse in Particle.java
const int PARTICLE_OFFSET = 512;                // Same in Particle.java
const float TARGET_TPS = 20.0;
const float NANOSECONDS_PER_SECOND = 1000000000;
const int PARTICLE_TIME_SHIFT = 20;

float getTimeScaler(Particle currentParticle, float aliveTime) {
    float maxLiveTime = float(currentParticle.packedLifeTimeRotationMaterial >> 24 & 0xFF) / TARGET_TPS;

    return max(0.0, (maxLiveTime - aliveTime) / maxLiveTime);
}

float getGravity(Particle currentParticle) {
    return (currentParticle.packedVelocityGravity & 0xFF) * GRAVITY_PACKING_FACTOR;
}

float getAliveTime(Particle currentParticle) {
    int aliveTime = currentTime - spawnTime;
    return float(aliveTime) * (float(1 << PARTICLE_TIME_SHIFT) / NANOSECONDS_PER_SECOND);
}

vec2 getRotationSpeed(Particle currentParticle) {
    float rotationSpeedX = (currentParticle.packedLifeTimeRotationMaterial >> 16 & 0xFF) * ROTATION_PACKING_FACTOR;
    float rotationSpeedY = (currentParticle.packedLifeTimeRotationMaterial >> 8 & 0xFF) * ROTATION_PACKING_FACTOR;

    return vec2(rotationSpeedX, rotationSpeedY);
}

vec3 getVelocity(Particle currentParticle) {
    float velocityX = ((currentParticle.packedVelocityGravity >> 24 & 0xFF) - 128) * VELOCITY_PACKING_FACTOR;
    float velocityY = ((currentParticle.packedVelocityGravity >> 16 & 0xFF) - 128) * VELOCITY_PACKING_FACTOR;
    float velocityZ = ((currentParticle.packedVelocityGravity >> 8 & 0xFF) - 128) * VELOCITY_PACKING_FACTOR;

    return vec3(velocityX, velocityY, velocityZ);
}

vec3 getFacePositions(int side, int currentVertexId) {
    vec3 currentVertexOffset = vec3(FACE_POSITIONS[currentVertexId].xy, 0);

    switch (side) {
        case 0: return currentVertexOffset.yxz + vec3(0, 0, 1);
        case 1: return currentVertexOffset.xzy + vec3(0, 1, 0);
        case 2: return currentVertexOffset.zyx + vec3(1, 0, 0);
        case 3: return currentVertexOffset.xyz;
        case 4: return currentVertexOffset.yzx;
        case 5: return currentVertexOffset.zxy;
    }

    return vec3(0, 0, 0);
}

int getWrappedPosition(int actualPosition, int reference, int wrappingDistance) {
    if (actualPosition - reference > wrappingDistance >> 1) return actualPosition - wrappingDistance;
    if (reference - actualPosition > wrappingDistance >> 1) return actualPosition + wrappingDistance;
    return actualPosition;
}

ivec3 getWrappedPosition(ivec3 worldPos) {
    return ivec3(
    getWrappedPosition(worldPos.x, iCameraPosition.x, 1 << 30),
    getWrappedPosition(worldPos.y, iCameraPosition.y, 1 << 22),
    getWrappedPosition(worldPos.z, iCameraPosition.z, 1 << 30)
    ) - iCameraPosition;
}

vec3 rotate(vec3 vector, Particle currentParticle, float aliveTime) {
    vec2 rotation = getRotationSpeed(currentParticle) * aliveTime;

    float cosValue = cos(rotation.x);
    float sinValue = sin(rotation.x);
    vector = mat3(1.0, 0.0, 0.0, 0.0, cosValue, -sinValue, 0.0, sinValue, cosValue) * vector;

    cosValue = cos(rotation.y);
    sinValue = sin(rotation.y);
    vector = mat3(cosValue, 0.0, sinValue, 0.0, 1.0, 0.0, -sinValue, 0.0, cosValue) * vector;

    return vector;
}

void main() {
    Particle currentParticle = particles[gl_InstanceID];
    int currentVertexId = gl_VertexID % 6;

    int x = (currentParticle.packedOffset >> 20 & 0x3FF) + startPosition.x - PARTICLE_OFFSET;
    int y = (currentParticle.packedOffset >> 10 & 0x3FF) + startPosition.y - PARTICLE_OFFSET;
    int z = (currentParticle.packedOffset >> 00 & 0x3FF) + startPosition.z - PARTICLE_OFFSET;
    int side = (gl_VertexID / 6) % 6;
    float aliveTime = getAliveTime(currentParticle);
    float timeScaler = getTimeScaler(currentParticle, aliveTime);

    ivec3 wrappedPositon = getWrappedPosition(ivec3(x, y, z));
    vec3 facePosition = getFacePositions(side, currentVertexId);
    vec3 position = vec3(wrappedPositon) + rotate(facePosition * timeScaler - vec3(timeScaler * 0.5), currentParticle, aliveTime) + vec3(timeScaler * 0.5);
    position += getVelocity(currentParticle) * aliveTime;
    position.y -= 0.5 * getGravity(currentParticle) * aliveTime * aliveTime;

    gl_Position = projectionViewMatrix * vec4(position, 1.0);

    textureData = side << 8 | currentParticle.packedLifeTimeRotationMaterial & 0xFF;
    totalPosition = wrappedPositon + facePosition;
    normal = rotate(NORMALS[side], currentParticle, aliveTime);
}