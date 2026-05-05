extern crate jni;

use jni::objects::{JByteArray, JClass, JIntArray, JPrimitiveArray};
use jni::sys::{jint, jintArray};
use jni::{AttachGuard, Env, EnvUnowned};
use jni::elements::{ReleaseMode};
use super::materials_data::{CHUNK_SIZE, CHUNK_SIZE_BITS, AIR, MaterialsData, NORTH, SOUTH, get_uncompressed_index, TOP, BOTTOM, WEST, EAST};

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_game_server_generation_NativeFunctions_generateMesh<'a>(
    env_unowned: EnvUnowned, _class: JClass,
    materials_data: JByteArray, surface_equivalent: JByteArray,
    north: JByteArray, top: JByteArray, west: JByteArray, south: JByteArray, bottom: JByteArray, east: JByteArray,
    x_start: jint, y_start: jint, z_start: jint) -> jintArray {
    // let function_start: Instant = Instant::now();

    let mut guard: AttachGuard = unsafe { AttachGuard::from_unowned(env_unowned.as_raw()) };
    let env: &mut Env = guard.borrow_env_mut();

    let mut materials_data: Vec<i8> = copy_from_java_array(&env, materials_data);
    let mut surface_equivalent: Vec<i8> = copy_from_java_array(&env, surface_equivalent);

    let mut north: Vec<i8> = copy_from_java_array(&env, north);
    let mut top: Vec<i8> = copy_from_java_array(&env, top);
    let mut west: Vec<i8> = copy_from_java_array(&env, west);
    let mut south: Vec<i8> = copy_from_java_array(&env, south);
    let mut bottom: Vec<i8> = copy_from_java_array(&env, bottom);
    let mut east: Vec<i8> = copy_from_java_array(&env, east);

    // println!("Got data ready in {}", function_start.elapsed().as_micros());
    // let start: Instant = Instant::now();

    let mesh_data: Vec<i32> = generate_mesh(materials_data.as_mut(), surface_equivalent.as_mut(),
                                            north.as_mut(), top.as_mut(), west.as_mut(), south.as_mut(), bottom.as_mut(), east.as_mut(),
                                            x_start, y_start, z_start);
    // println!("Meshed Rust side in {}", start.elapsed().as_micros());

    let result: JPrimitiveArray<jint> = JIntArray::new(env, mesh_data.len()).unwrap();

    let java_array = unsafe { result.get_elements_critical(&env, ReleaseMode::CopyBack) }.unwrap();
    unsafe { std::ptr::copy_nonoverlapping(mesh_data.as_ptr(), java_array.as_ptr(), mesh_data.len()); }

    result.as_raw() as jintArray
}

fn copy_from_java_array(env: &Env, array: JByteArray) -> Vec<i8> {
    let auto_elements = unsafe { array.get_elements(&env, ReleaseMode::NoCopyBack) };
    auto_elements.unwrap().to_vec()
}

pub fn is_visible(to_test_material: i8, occluding_material: i8) -> bool {
    if to_test_material == AIR { return false; }
    if occluding_material == AIR { return true; }

    if (MATERIAL_PROPERTIES[occluding_material as u8 as usize] & TRANSPARENT) == 0 { return false; }
    if (MATERIAL_PROPERTIES[to_test_material as u8 as usize] & OCCLUDES_SELF_ONLY) == OCCLUDES_SELF_ONLY {
        return to_test_material != occluding_material;
    }

    true
}

fn generate_mesh(materials_data: &[i8], surface_equivalent: &[i8],
                 north: &[i8], top: &[i8], west: &[i8], south: &[i8], bottom: &[i8], east: &[i8],
                 x_start: i32, y_start: i32, z_start: i32) -> Vec<i32> {
    let mut generator: MeshGenerator = MeshGenerator::new(x_start, y_start, z_start);
    let mut to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6] = &mut [0; CHUNK_SIZE * CHUNK_SIZE * 6];

    let adjacent_chunk_layers: &[&[i8]; 6] = &[north, top, west, south, bottom, east];

    let materials: MaterialsData = MaterialsData { data: materials_data };
    let surface_equivalent: MaterialsData = MaterialsData { data: surface_equivalent };

    // let uncompressed: Instant = Instant::now();
    materials.fill_uncompressed_materials(&mut generator.uncompressed_materials, CHUNK_SIZE_BITS as i32, 0, 0, 0, 0);
    // println!("Rust uncompressed in {}", uncompressed.elapsed().as_nanos());
    // let bitmap: Instant = Instant::now();
    surface_equivalent.generate_to_mesh_faces_maps(&mut to_mesh_faces_map, &mut generator.uncompressed_materials, adjacent_chunk_layers, CHUNK_SIZE_BITS as i32, 0, 0, 0, 0);
    // println!("Rust bitmap in {}", bitmap.elapsed().as_nanos());
    // let mesh: Instant = Instant::now();
    generator.generate_mesh(to_mesh_faces_map);
    // println!("Rust mesh in {}", mesh.elapsed().as_nanos());
    generator.get_mesh_data()
}

struct MeshGenerator {
    uncompressed_materials: [i8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
    materials_layer: [i8; CHUNK_SIZE * CHUNK_SIZE],
    vertex_counts: [i32; 9],
    x_start: i32,
    y_start: i32,
    z_start: i32,
}

static mut VERTICES: [Vec<i32>; 4] = [Vec::new(), Vec::new(), Vec::new(), Vec::new()];

impl MeshGenerator {
    fn new(x_start: i32, y_start: i32, z_start: i32) -> MeshGenerator {
        MeshGenerator {
            uncompressed_materials: [AIR; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
            materials_layer: [AIR; CHUNK_SIZE * CHUNK_SIZE],
            vertex_counts: [0; 9],
            x_start,
            y_start,
            z_start,
        }
    }

    fn get_mesh_data(&mut self) -> Vec<i32> {
        unsafe {
            let mut required_length: usize = 9;
            required_length += VERTICES[0].len() + VERTICES[1].len() + VERTICES[2].len() + VERTICES[3].len();
            let mut mesh_data: Vec<i32> = Vec::with_capacity(required_length);

            self.vertex_counts.iter().for_each(|vertex_count: &i32| { mesh_data.push(*vertex_count); });
            mesh_data.append(&mut VERTICES[0]);
            mesh_data.append(&mut VERTICES[1]);
            mesh_data.append(&mut VERTICES[2]);
            mesh_data.append(&mut VERTICES[3]);

            mesh_data
        }
    }

    fn generate_mesh(&mut self, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        self.add_north_south_faces(to_mesh_faces_maps);
        self.add_top_bottom_faces(to_mesh_faces_maps);
        self.add_west_east_faces(to_mesh_faces_maps);
        if self.has_opaque_mesh() { self.add_side_layers() }
    }


    fn add_north_south_faces(&mut self, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        for material_z in 0..CHUNK_SIZE as i32 {
            self.copy_materials_north_south(material_z as i32, to_mesh_faces_maps);
            self.add_north_south_layer(NORTH, material_z as i32, to_mesh_faces_maps);
            self.add_north_south_layer(SOUTH, material_z as i32, to_mesh_faces_maps);
        }
    }

    fn add_top_bottom_faces(&mut self, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        for material_y in 0..CHUNK_SIZE as i32 {
            self.copy_materials_top_bottom(material_y as i32, to_mesh_faces_maps);
            self.add_top_bottom_layer(TOP, material_y as i32, to_mesh_faces_maps);
            self.add_top_bottom_layer(BOTTOM, material_y as i32, to_mesh_faces_maps);
        }
    }

    fn add_west_east_faces(&mut self, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        for material_x in 0..CHUNK_SIZE as i32 {
            self.copy_materials_west_east(material_x as i32, to_mesh_faces_maps);
            self.add_west_east_layer(WEST, material_x as i32, to_mesh_faces_maps);
            self.add_west_east_layer(EAST, material_x as i32, to_mesh_faces_maps);
        }
    }


    fn copy_materials_north_south(&mut self, material_z: i32, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_1: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_maps, NORTH, material_z);
        let to_mesh_faces_2: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_maps, SOUTH, material_z);

        for material_x in 0..CHUNK_SIZE as i32 {
            let mut required_materials: u64 = to_mesh_faces_1[material_x as usize] | to_mesh_faces_2[material_x as usize];
            let mut material_y: i32 = required_materials.trailing_zeros() as i32;

            while material_y < CHUNK_SIZE as i32 {
                self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_y as usize] = self.uncompressed_materials[get_uncompressed_index(material_x as i32, material_y, material_z)];
                required_materials &= (-2i64 as u64) << material_y;
                material_y = required_materials.trailing_zeros() as i32;
            }
        }
    }

    fn copy_materials_top_bottom(&mut self, material_y: i32, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_1: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_maps, TOP, material_y);
        let to_mesh_faces_2: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_maps, BOTTOM, material_y);

        for material_x in 0..CHUNK_SIZE as i32 {
            let mut required_materials: u64 = to_mesh_faces_1[material_x as usize] | to_mesh_faces_2[material_x as usize];
            let mut material_z: i32 = required_materials.trailing_zeros() as i32;

            while material_z < CHUNK_SIZE as i32 {
                self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_z as usize] = self.uncompressed_materials[get_uncompressed_index(material_x as i32, material_y, material_z)];
                required_materials &= (-2i64 as u64) << material_z;
                material_z = required_materials.trailing_zeros() as i32;
            }
        }
    }

    fn copy_materials_west_east(&mut self, material_x: i32, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_1: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_maps, WEST, material_x);
        let to_mesh_faces_2: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_maps, EAST, material_x);

        for material_z in 0..CHUNK_SIZE as i32 {
            let mut required_materials: u64 = to_mesh_faces_1[material_z as usize] | to_mesh_faces_2[material_z as usize];
            let mut material_y: i32 = required_materials.trailing_zeros() as i32;

            while material_y < CHUNK_SIZE as i32 {
                self.materials_layer[(material_z as usize) << CHUNK_SIZE_BITS | material_y as usize] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z as i32)];
                required_materials &= (-2i64 as u64) << material_y;
                material_y = required_materials.trailing_zeros() as i32;
            }
        }
    }


    fn add_north_south_layer(&mut self, side: usize, material_z: i32, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = MaterialsData::split_map(to_mesh_faces_maps, side, material_z);
        for material_x in 0..CHUNK_SIZE as i32 {
            let mut material_y: i32 = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;

            while material_y < CHUNK_SIZE as i32 {
                let material: i8 = self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_y as usize];
                let face_end_y: i32 = self.grow_face_1st_direction(to_mesh_faces_map[material_x as usize], material_y + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_x: i32 = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_x - material_x);
                material_y = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;
            }
        }
    }

    fn add_top_bottom_layer(&mut self, side: usize, material_y: i32, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = MaterialsData::split_map(to_mesh_faces_maps, side, material_y);
        for material_x in 0..CHUNK_SIZE as i32 {
            let mut material_z: i32 = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;

            while material_z < CHUNK_SIZE as i32 {
                let material: i8 = self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_z as usize];
                let face_end_z: i32 = self.grow_face_1st_direction(to_mesh_faces_map[material_x as usize], material_z + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_z - material_z + 1, material_z);
                let face_end_x: i32 = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_z, face_end_z, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_face(side, material_x, material_y, material_z, material, face_end_x - material_x, face_end_z - material_z);
                material_z = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;
            }
        }
    }

    fn add_west_east_layer(&mut self, side: usize, material_x: i32, to_mesh_faces_maps: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = MaterialsData::split_map(to_mesh_faces_maps, side, material_x);
        for material_z in 0..CHUNK_SIZE as i32 {
            let mut material_y: i32 = to_mesh_faces_map[material_z as usize].trailing_zeros() as i32;

            while material_y < CHUNK_SIZE as i32 {
                let material: i8 = self.materials_layer[(material_z as usize) << CHUNK_SIZE_BITS | material_y as usize];
                let face_end_y: i32 = self.grow_face_1st_direction(to_mesh_faces_map[material_z as usize], material_y + 1, material_z, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_z: i32 = self.grow_face_2nd_direction(to_mesh_faces_map, material_z + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_z, face_end_z);
                self.add_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_z - material_z);
                material_y = to_mesh_faces_map[material_z as usize].trailing_zeros() as i32;
            }
        }
    }


    fn add_face(&mut self, side: usize, material_x: i32, material_y: i32, material_z: i32, material: i8, face_size_1: i32, face_size_2: i32) {
        if MeshGenerator::is_glass(material) {
            self.add_face_to_vertices(3, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
            self.vertex_counts[8] += 3;
        } else if material == WATER {
            self.add_face_to_vertices(2, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
            self.vertex_counts[7] += 3;
        } else {
            let index: usize = if side >= SOUTH { 1 } else { 0 };
            self.add_face_to_vertices(index, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
            self.vertex_counts[side] += 3;
        }
    }

    fn add_face_to_vertices(&mut self, vertices_index: usize, side: usize, material_x: i32, material_y: i32, material_z: i32, material: i8, face_size_1: i32, face_size_2: i32) {
        unsafe {
            VERTICES[vertices_index].push(self.x_start | material_x);
            VERTICES[vertices_index].push(self.y_start | material_y);
            VERTICES[vertices_index].push(self.z_start | material_z);
            VERTICES[vertices_index].push((MATERIAL_PROPERTIES[material as u8 as usize] as i32) << 24 | face_size_1 << 17 | face_size_2 << 11 | (side as i32) << 8 | (material as u8 as i32));
        }
    }

    fn grow_face_1st_direction(&self, to_mesh_faces: u64, mut grow_start: i32, fixed_start: i32, material: i8) -> i32 {
        while grow_start < CHUNK_SIZE as i32 {
            let index: i32 = fixed_start << CHUNK_SIZE_BITS | grow_start;
            if to_mesh_faces & 1 << grow_start == 0 || self.materials_layer[index as usize] != material { return grow_start - 1; }
            grow_start += 1;
        }
        CHUNK_SIZE as i32 - 1
    }

    fn grow_face_2nd_direction(&self, to_mesh_faces_map: &[u64; CHUNK_SIZE], mut grow_start: i32, mask: u64, fixed_start: i32, fixed_end: i32, material: i8) -> i32 {
        while grow_start < CHUNK_SIZE as i32 && (to_mesh_faces_map[grow_start as usize] & mask) == mask {
            for index in fixed_start..=fixed_end {
                if self.materials_layer[(grow_start as usize) << CHUNK_SIZE_BITS | index as usize] != material { return grow_start - 1; }
            }
            grow_start += 1;
        }
        grow_start - 1
    }

    fn remove_from_map(to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut mask: u64, start: i32, end: i32) {
        mask = !mask;
        for index in start..=end {
            to_mesh_faces_map[index as usize] &= mask;
        }
    }

    fn is_glass(material: i8) -> bool {
        material as u8 >= 125 && material as u8 <= 132
    }

    fn split_map(to_mesh_faces_maps: &[u64; CHUNK_SIZE * CHUNK_SIZE * 6], side: usize, in_chunk: i32) -> &[u64; CHUNK_SIZE] {
        let index: usize = side as usize * CHUNK_SIZE * CHUNK_SIZE + in_chunk as usize * CHUNK_SIZE;
        <&[u64; CHUNK_SIZE]>::try_from(&to_mesh_faces_maps[index..index + CHUNK_SIZE]).unwrap()
    }

    fn has_opaque_mesh(&self) -> bool {
        unsafe {
            for vertices_vec in &VERTICES[0..2] {
                if vertices_vec.len() != 0 { return true; }
            }
        }
        false
    }
}

impl MeshGenerator {
    fn add_side_layers(&mut self) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = &mut [u64::MAX; CHUNK_SIZE];
        self.copy_materials_north_south_side_layer(0);
        self.add_north_south_side_layer(SOUTH, 0, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_north_south_side_layer(CHUNK_SIZE as i32 - 1);
        self.add_north_south_side_layer(NORTH, CHUNK_SIZE as i32 - 1, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_top_bottom_side_layer(0);
        self.add_top_bottom_side_layer(BOTTOM, 0, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_top_bottom_side_layer(CHUNK_SIZE as i32 - 1);
        self.add_top_bottom_side_layer(TOP, CHUNK_SIZE as i32 - 1, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_west_east_side_layer(0);
        self.add_west_east_side_layer(EAST, 0, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_west_east_side_layer(CHUNK_SIZE as i32 - 1);
        self.add_west_east_side_layer(WEST, CHUNK_SIZE as i32 - 1, to_mesh_faces_map);
    }

    fn add_side_face(&mut self, side: usize, material_x: i32, material_y: i32, material_z: i32, material: i8, face_size_1: i32, face_size_2: i32) {
        if MATERIAL_PROPERTIES[material as u8 as usize] & TRANSPARENT != 0 { return; }
        self.add_face_to_vertices(1, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
        self.vertex_counts[6] += 3;
    }


    fn copy_materials_north_south_side_layer(&mut self, material_z: i32) {
        for material_x in 0..CHUNK_SIZE as i32 {
            for material_y in 0..CHUNK_SIZE as i32 {
                self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_y as usize] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
            }
        }
    }

    fn copy_materials_top_bottom_side_layer(&mut self, material_y: i32) {
        for material_x in 0..CHUNK_SIZE as i32 {
            for material_z in 0..CHUNK_SIZE as i32 {
                self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_z as usize] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
            }
        }
    }

    fn copy_materials_west_east_side_layer(&mut self, material_x: i32) {
        for material_z in 0..CHUNK_SIZE as i32 {
            for material_y in 0..CHUNK_SIZE as i32 {
                self.materials_layer[(material_z as usize) << CHUNK_SIZE_BITS | material_y as usize] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
            }
        }
    }


    fn add_north_south_side_layer(&mut self, side: usize, material_z: i32, to_mesh_faces_map: &mut [u64; CHUNK_SIZE]) {
        for material_x in 0..CHUNK_SIZE as i32 {
            let mut material_y: i32 = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;

            while material_y < CHUNK_SIZE as i32 {
                let material: i8 = self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_y as usize];
                let face_end_y: i32 = self.grow_face_1st_direction(to_mesh_faces_map[material_x as usize], material_y + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_x: i32 = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_side_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_x - material_x);
                material_y = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;
            }
        }
    }

    fn add_top_bottom_side_layer(&mut self, side: usize, material_y: i32, to_mesh_faces_map: &mut [u64; CHUNK_SIZE]) {
        for material_x in 0..CHUNK_SIZE as i32 {
            let mut material_z: i32 = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;

            while material_z < CHUNK_SIZE as i32 {
                let material: i8 = self.materials_layer[(material_x as usize) << CHUNK_SIZE_BITS | material_z as usize];
                let face_end_z: i32 = self.grow_face_1st_direction(to_mesh_faces_map[material_x as usize], material_z + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_z - material_z + 1, material_z);
                let face_end_x: i32 = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_z, face_end_z, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_side_face(side, material_x, material_y, material_z, material, face_end_x - material_x, face_end_z - material_z);
                material_z = to_mesh_faces_map[material_x as usize].trailing_zeros() as i32;
            }
        }
    }

    fn add_west_east_side_layer(&mut self, side: usize, material_x: i32, to_mesh_faces_map: &mut [u64; CHUNK_SIZE]) {
        for material_z in 0..CHUNK_SIZE as i32 {
            let mut material_y: i32 = to_mesh_faces_map[material_z as usize].trailing_zeros() as i32;

            while material_y < CHUNK_SIZE as i32 {
                let material: i8 = self.materials_layer[(material_z as usize) << CHUNK_SIZE_BITS | material_y as usize];
                let face_end_y: i32 = self.grow_face_1st_direction(to_mesh_faces_map[material_z as usize], material_y + 1, material_z, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_z: i32 = self.grow_face_2nd_direction(to_mesh_faces_map, material_z + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_z, face_end_z);
                self.add_side_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_z - material_z);
                material_y = to_mesh_faces_map[material_z as usize].trailing_zeros() as i32;
            }
        }
    }
}

static MATERIAL_PROPERTIES: [u32; 140] = [11, 6, 0, 0, 15, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 14, 14, 14, 14, 14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0];

const TRANSPARENT: u32 = 2;
const OCCLUDES_SELF_ONLY: u32 = 6;

const WATER: i8 = 4;

