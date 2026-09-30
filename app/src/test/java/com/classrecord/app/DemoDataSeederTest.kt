package com.classrecord.app

import com.classrecord.app.data.db.AppDatabase
import com.classrecord.app.data.demo.DemoDataSeeder
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.ClassRepository
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository
import com.classrecord.app.i18n.AppStrings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DemoDataSeederTest {
    private lateinit var db: AppDatabase
    private lateinit var seeder: DemoDataSeeder
    private lateinit var classRepository: ClassRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        if (::db.isInitialized) {
            db.close()
        }
        context.deleteDatabase("class_record.db")
        db = AppDatabase.create(context)
        val strings = AppStrings(context)
        classRepository = ClassRepository(db)
        seeder = DemoDataSeeder(
            db,
            classRepository,
            MemberRepository(db),
            SubGroupRepository(db),
            ActivityRepository(db, strings),
            LedgerRepository(db, strings),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedsEnglishPackWhenRequested() = runBlocking {
        assertTrue(seeder.seedIfEmpty(english = true))
        assertEquals("Weekend Badminton Club", classRepository.get()?.name)
        assertTrue(!seeder.canSeed())
    }

    @Test
    fun doesNotOverwriteExistingClass() = runBlocking {
        classRepository.saveName("Existing class")
        assertTrue(!seeder.seedIfEmpty(english = true))
    }
}
