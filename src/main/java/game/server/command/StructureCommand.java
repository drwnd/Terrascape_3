package game.server.command;

import core.assets.AssetManager;
import core.settings.optionSettings.ColorOption;
import core.utils.MathUtils;
import core.utils.Vector3l;

import game.assets.StructureIdentifier;
import game.player.Player;
import game.player.interaction.PlacingState;
import game.player.interaction.RepeatPlaceable;
import game.player.interaction.StructureSelector;
import game.player.interaction.Target;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.materials_data.MaterialsData;
import game.server.saving.StructureSaver;
import game.utils.Utils;

import java.nio.file.Path;

import static game.utils.Constants.*;

public final class StructureCommand {

    static final String SYNTAX = "info | (save \"Structure Name\" [centerX centerY centerZ] [force])";
    static final String EXPLANATION = "Saves the selected Region as a structure with the given name";

    static CommandResult execute(TokenList tokens) {
        String action = tokens.expectNextKeyWord().keyword();
        if ("info".equalsIgnoreCase(action)) return executeInfoAction(tokens);
        if ("save".equalsIgnoreCase(action)) return executeSaveAction(tokens);
        return CommandResult.fail("unexpected keyword: " + action);
    }

    private static CommandResult executeInfoAction(TokenList tokens) {
        tokens.expectFinishedLess();

        Vector3l minPosition = new Vector3l(), maxPosition = new Vector3l();
        CommandResult positionResult = getMinMaxPosition(minPosition, maxPosition);
        if (!positionResult.successful()) return positionResult;

        int sizeX = (int) (maxPosition.x - minPosition.x);
        int sizeY = (int) (maxPosition.y - minPosition.y);
        int sizeZ = (int) (maxPosition.z - minPosition.z);

        Game.getServer().sendServerMessage("SizeX: %d, SizeY: %d, SizeZ: %d".formatted(sizeX, sizeY, sizeZ), ColorOption.GREEN);
        return CommandResult.success();
    }

    private static CommandResult executeSaveAction(TokenList tokens) {
        String fileName = Utils.sanitizeFileName(tokens.expectNextString().string());
        Path saveFileLocation = StructureSaver.getSaveFileLocation(fileName);
        boolean forceSave = false, centerDefined = false;
        int centerX = 0, centerY = 0, centerZ = 0;

        if (tokens.nextIncrementNumber() instanceof NumberToken x) {
            NumberToken y = tokens.expectGetNumber();
            NumberToken z = tokens.expectNextNumber();
            tokens.next();
            if (!x.isInteger() || x.number() < 0 || !y.isInteger() || y.number() < 0 || !z.isInteger() || z.number() < 0)
                return CommandResult.fail("Center XYZ must be positive Integers");
            centerX = (int) x.number();
            centerY = (int) y.number();
            centerZ = (int) z.number();
            centerDefined = true;
        }

        if (tokens.getIncrementKeyword() instanceof KeywordToken(String keyword)) {
            if ("force".equalsIgnoreCase(keyword)) forceSave = true;
            else return CommandResult.fail("Unexpected keyword: " + keyword);
        }

        return saveStructure(tokens, centerDefined, centerX, centerY, centerZ, forceSave, saveFileLocation, fileName);
    }

    private static CommandResult saveStructure(TokenList tokens, boolean centerDefined, int centerX, int centerY, int centerZ, boolean forceSave, Path saveFileLocation, String fileName) {
        tokens.expectFinishedLess();
        if (!forceSave && saveFileLocation.toFile().exists())
            return CommandResult.fail("That structure already exists. Choose another name or override with /%s force".formatted(tokens.getCommand()));

        Vector3l minPosition = new Vector3l(), maxPosition = new Vector3l();
        CommandResult positionResult = getMinMaxPosition(minPosition, maxPosition);
        if (!positionResult.successful()) return positionResult;

        int sizeX = (int) (maxPosition.x - minPosition.x);
        int sizeY = (int) (maxPosition.y - minPosition.y);
        int sizeZ = (int) (maxPosition.z - minPosition.z);

        if (!centerDefined) {
            centerX = sizeX >> 1;
            centerY = 0;
            centerZ = sizeZ >> 1;
        } else if (centerX >= sizeX || centerY >= sizeY || centerZ >= sizeZ)
            return CommandResult.fail("CenterXYZ must be within the bounds of the Structure");

        if (sizeX > MAX_STRUCTURE_SIZE || sizeY > MAX_STRUCTURE_SIZE || sizeZ > MAX_STRUCTURE_SIZE)
            return CommandResult.fail("Structure cannot be larger than %d voxels along one axis".formatted(MAX_STRUCTURE_SIZE));

        int sizeBits = Integer.numberOfTrailingZeros(MathUtils.nextLargestPowOf2(MathUtils.max(sizeX, sizeY, sizeZ)));
        byte[] uncompressedMaterials = new byte[1 << sizeBits * 3];

        World world = Game.getWorld();
        for (int structureX = 0; structureX < sizeX; structureX++)
            for (int structureY = 0; structureY < sizeY; structureY++)
                for (int structureZ = 0; structureZ < sizeZ; structureZ++) {
                    byte material = world.getMaterial(minPosition.x + structureX, minPosition.y + structureY, minPosition.z + structureZ, 0);
                    int index = MaterialsData.getUncompressedIndex(structureX, structureY, structureZ);
                    uncompressedMaterials[index] = material;
                }

        Structure structure = new Structure(sizeX, sizeY, sizeZ, centerX, centerY, centerZ, MaterialsData.getCompressedMaterials(sizeBits, uncompressedMaterials));
        StructureSaver structureSaver = new StructureSaver();

        structureSaver.save(structure, saveFileLocation);
        AssetManager.delete(new StructureIdentifier(fileName));
        return CommandResult.success();
    }

    private static CommandResult getMinMaxPosition(Vector3l minPosition, Vector3l maxPosition) {
        Player player = Game.getPlayer();
        Target lockedTarget = player.getInteractionHandler().getLockedTarget();
        Target startTarget = player.getInteractionHandler().getStartTarget();
        if (startTarget == null) startTarget = lockedTarget;
        PlacingState state = player.getInteractionHandler().getState(Target.getPlayerTarget());

        if (!(player.getHeldPlaceable() instanceof StructureSelector))
            return CommandResult.fail("Must use a Structure Selector");
        if (state != PlacingState.STRUCTURE_SELECT_LOCKED)
            return CommandResult.fail("Must have locked a region to save as a structure");

        Vector3l startPositon = startTarget.position();
        Vector3l endPosition = lockedTarget.position();

        RepeatPlaceable.offsetPositions(startPositon, endPosition, startTarget.side(), null);
        minPosition.set(Utils.min(startPositon, endPosition));
        maxPosition.set(Utils.max(startPositon, endPosition));
        maxPosition.add(1, 1, 1);
        return CommandResult.success();
    }
}
